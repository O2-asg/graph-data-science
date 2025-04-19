/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.compat;

import org.jetbrains.annotations.TestOnly;
import org.neo4j.common.DependencyResolver;
import org.neo4j.configuration.BootloaderSettings;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.configuration.connectors.ConnectorPortRegister;
import org.neo4j.configuration.connectors.ConnectorType;
import org.neo4j.configuration.helpers.DatabaseNameValidator;
import org.neo4j.gds.annotation.SuppressForbidden;
import org.neo4j.gds.compat.batchimport.BatchImporter;
import org.neo4j.gds.compat.batchimport.ExecutionMonitor;
import org.neo4j.gds.compat.batchimport.ImportConfig;
import org.neo4j.gds.compat.batchimport.input.Collector;
import org.neo4j.gds.compat.batchimport.input.Estimates;
import org.neo4j.gds.compat.batchimport.input.ReadableGroups;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.internal.helpers.HostnamePort;
import org.neo4j.internal.id.IdGenerator;
import org.neo4j.internal.id.IdGeneratorFactory;
import org.neo4j.internal.kernel.api.Cursor;
import org.neo4j.internal.kernel.api.EntityCursor;
import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.NodeLabelIndexCursor;
import org.neo4j.internal.kernel.api.PartitionedScan;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.internal.kernel.api.Read;
import org.neo4j.internal.kernel.api.RelationshipScanCursor;
import org.neo4j.internal.kernel.api.connectioninfo.ClientConnectionInfo;
import org.neo4j.internal.kernel.api.exceptions.InvalidTransactionTypeKernelException;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.internal.kernel.api.security.AccessMode;
import org.neo4j.internal.kernel.api.security.AuthSubject;
import org.neo4j.internal.kernel.api.security.AuthenticationResult;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.internal.recordstorage.RecordIdType;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.layout.Neo4jLayout;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.KernelTransactionHandle;
import org.neo4j.kernel.api.procedure.CallableProcedure;
import org.neo4j.kernel.api.procedure.Context;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.database.DatabaseReferenceImpl;
import org.neo4j.kernel.database.DatabaseReferenceRepository;
import org.neo4j.kernel.database.NormalizedDatabaseName;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.query.QueryExecutionConfiguration;
import org.neo4j.kernel.impl.query.TransactionalContext;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;
import org.neo4j.logging.InternalLog;
import org.neo4j.logging.Log;
import org.neo4j.logging.internal.LogService;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.ssl.config.SslPolicyLoader;
import org.neo4j.storageengine.api.LongReference;
import org.neo4j.storageengine.api.PropertySelection;
import org.neo4j.storageengine.api.Reference;
import org.neo4j.storageengine.api.StorageEngineFactory;
import org.neo4j.values.storable.TextArray;
import org.neo4j.values.virtual.MapValue;
import org.neo4j.values.virtual.NodeValue;
import org.neo4j.values.virtual.VirtualValues;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

import static java.lang.String.format;

public final class Neo4jProxy {

    private static final Neo4jProxyApi IMPL = ProxyUtil.findProxy(
        Neo4jProxyFactory.class,
        ProxyUtil.MayLogToStdout.YES
    );

    public static long estimateNodeCount(Read read, int label) {
        return read.estimateCountsForNode(label);
    }

    public static long estimateRelationshipCount(Read read, int sourceLabel, int targetLabel, int type) {
        return read.estimateCountsForRelationships(sourceLabel, type, targetLabel);
    }

    public static DependencyResolver emptyDependencyResolver() {
        return new DependencyResolver() {
            @Override
            public <T> T resolveDependency(Class<T> type, SelectionStrategy selector) {
                return null;
            }

            @Override
            public boolean containsDependency(Class<?> type) {
                return false;
            }
        };
    }

    public static CompatExecutionContext executionContext(KernelTransaction ktx) {
        var stmt = ktx.acquireStatement();
        var ctx = ktx.createExecutionContext();
        return new CompatExecutionContext() {
            @Override
            public CursorContext cursorContext() {
                return ctx.cursorContext();
            }

            @Override
            public AccessMode accessMode() {
                return ctx.securityContext().mode();
            }

            @Override
            public <C extends Cursor> boolean reservePartition(PartitionedScan<C> scan, C cursor) {
                return scan.reservePartition(cursor, ctx);
            }

            @Override
            public void close() {
                ctx.complete();
                ctx.close();
                stmt.close();
            }
        };
    }

    public static ProcedureSignatureBuilder procedureSignature() {
        return ProcedureSignatureBuilder.builder().compat(IMPL);
    }

    public static UserFunctionSignatureBuilder userFunctionSignature() {
        return UserFunctionSignatureBuilder.builder().compat(IMPL);
    }

    public static BoltTransactionRunner boltTransactionRunner() {
        return new BoltTransactionRunner();
    }

    public static boolean isCompositeDatabase(GraphDatabaseService databaseService) {
        var databaseId = GraphDatabaseApiProxy.databaseId(databaseService);
        var repo = GraphDatabaseApiProxy.resolveDependency(databaseService, DatabaseReferenceRepository.class);
        return repo.getCompositeDatabaseReferences()
            .stream()
            .map(DatabaseReferenceImpl.Internal::databaseId)
            .anyMatch(databaseId::equals);
    }

    public static <T> T lookupComponentProvider(Context ctx, Class<T> component, boolean safe)
        throws ProcedureException {
        var globalProcedures = GraphDatabaseApiProxy.resolveDependency(
            ctx.dependencyResolver(),
            GlobalProcedures.class
        );
        return globalProcedures.getCurrentView().lookupComponentProvider(component, safe).apply(ctx);
    }

    public static GlobalProcedureRegistry globalProcedureRegistry(GlobalProcedures globalProcedures) {
        return IMPL.globalProcedureRegistry(globalProcedures);
    }

    public static String validateExternalDatabaseName(String databaseName) {
        var normalizedName = new NormalizedDatabaseName(databaseName);
        DatabaseNameValidator.validateExternalDatabaseName(normalizedName);
        return normalizedName.name();
    }

    public static String username(AuthSubject subject) {
        return subject.executingUser();
    }

    // Maybe we should move this to a test-only proxy?
    @TestOnly
    public static SecurityContext securityContext(
        String username,
        AuthSubject authSubject,
        AccessMode mode,
        String databaseName
    ) {
        return new SecurityContext(
            new AuthSubject() {
                @Override
                public AuthenticationResult getAuthenticationResult() {
                    return authSubject.getAuthenticationResult();
                }

                @Override
                public boolean hasUsername(String s) {
                    return s.equals(username);
                }

                @Override
                public String executingUser() {
                    return username;
                }
            },
            mode,
            // GDS is always operating from an embedded context
            ClientConnectionInfo.EMBEDDED_CONNECTION,
            databaseName
        );
    }

    public static Write dataWrite(KernelTransaction kernelTransaction) throws InvalidTransactionTypeKernelException {
        return IMPL.dataWrite(kernelTransaction);
    }

    public static PropertyCursor allocatePropertyCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction
            .cursors()
            .allocatePropertyCursor(kernelTransaction.cursorContext(), kernelTransaction.memoryTracker());
    }

    public static Reference propertyReference(EntityCursor nodeCursor) {
        return nodeCursor.propertiesReference();
    }

    public static Reference noPropertyReference() {
        return LongReference.NULL_REFERENCE;
    }

    public static void nodeProperties(
        KernelTransaction kernelTransaction,
        long nodeReference,
        Reference reference,
        PropertyCursor cursor
    ) {
        kernelTransaction
            .dataRead()
            .nodeProperties(nodeReference, reference, PropertySelection.ALL_PROPERTIES, cursor);
    }

    public static void relationshipProperties(
        KernelTransaction kernelTransaction,
        long relationshipReference,
        long sourceNodeReference,
        Reference reference,
        PropertyCursor cursor
    ) {
        IMPL.relationshipProperties(
            kernelTransaction.dataRead(),
            relationshipReference,
            sourceNodeReference,
            reference,
            PropertySelection.ALL_PROPERTIES,
            cursor
        );
    }

    public static NodeCursor allocateNodeCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateNodeCursor(kernelTransaction.cursorContext());
    }

    public static RelationshipScanCursor allocateRelationshipScanCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateRelationshipScanCursor(kernelTransaction.cursorContext());
    }

    public static NodeLabelIndexCursor allocateNodeLabelIndexCursor(KernelTransaction kernelTransaction) {
        return kernelTransaction.cursors().allocateNodeLabelIndexCursor(kernelTransaction.cursorContext());
    }

    public static boolean hasNodeLabelIndex(KernelTransaction kernelTransaction) {
        return NodeLabelIndexLookupImpl.hasNodeLabelIndex(kernelTransaction);
    }

    public static StoreScan<NodeLabelIndexCursor> nodeLabelIndexScan(
        KernelTransaction transaction,
        int labelId,
        int batchSize
    ) {
        return PartitionedStoreScan.createScans(transaction, batchSize, labelId).get(0);
    }

    public static StoreScan<NodeCursor> nodesScan(KernelTransaction ktx, long nodeCount, int batchSize) {
        int numberOfPartitions = PartitionedStoreScan.getNumberOfPartitions(nodeCount, batchSize);
        return new PartitionedStoreScan<>(ktx.dataRead().allNodesScan(numberOfPartitions, ktx.cursorContext()));
    }

    public static StoreScan<RelationshipScanCursor> relationshipsScan(
        KernelTransaction ktx,
        long relationshipCount,
        int batchSize
    ) {
        int numberOfPartitions = PartitionedStoreScan.getNumberOfPartitions(relationshipCount, batchSize);
        return new PartitionedStoreScan<>(ktx.dataRead().allRelationshipsScan(numberOfPartitions, ktx.cursorContext()));
    }

    public static CompositeNodeCursor compositeNodeCursor(List<NodeLabelIndexCursor> cursors, int[] labelIds) {
        return new CompositeNodeCursorImpl(cursors, labelIds);
    }

    public static BatchImporter instantiateBatchImporter(
        DatabaseLayout directoryStructure,
        FileSystemAbstraction fileSystem,
        ImportConfig configuration,
        LogService logService,
        ExecutionMonitor executionMonitor,
        Config dbConfig,
        JobScheduler jobScheduler,
        Collector badCollector
    ) {
        if (dbConfig.get(GraphDatabaseSettings.db_format).equals("block")) {
            return IMPL.instantiateBlockBatchImporter(
                directoryStructure,
                fileSystem,
                configuration,
                executionMonitor.toMonitor(),
                logService,
                dbConfig,
                jobScheduler,
                badCollector
            );
        } else {
            return IMPL.instantiateRecordBatchImporter(
                directoryStructure,
                fileSystem,
                configuration,
                executionMonitor,
                logService,
                dbConfig,
                jobScheduler,
                badCollector
            );
        }
    }

    public static Setting<String> additionalJvm() {
        return BootloaderSettings.additional_jvm;
    }

    public static Setting<Long> pageCacheMemory() {
        return GraphDatabaseSettings.pagecache_memory;
    }

    public static Long pageCacheMemoryValue(String value) {
        return SettingValueParsers.BYTES.parse(value);
    }

    public static long getHighestPossibleNodeCount(IdGeneratorFactory idGeneratorFactory) {
        return InternalReadOps.findValidIdGeneratorsStream(
                idGeneratorFactory,
                RecordIdType.NODE,
                BlockFormat.INSTANCE.nodeType,
                BlockFormat.INSTANCE.dynamicNodeType
            )
            .mapToLong(IdGenerator::getHighId)
            .max()
            .orElseThrow(InternalReadOps::unsupportedStoreFormatException);
    }

    public static long getHighestPossibleRelationshipCount(Read read) {
        return read.relationshipsGetCount();
    }

    public static ExecutionMonitor newCoarseBoundedProgressExecutionMonitor(
        long highNodeId,
        long highRelationshipId,
        int batchSize,
        LongConsumer progress,
        LongConsumer outNumberOfBatches
    ) {
        return IMPL.newCoarseBoundedProgressExecutionMonitor(
            highNodeId,
            highRelationshipId,
            batchSize,
            progress,
            outNumberOfBatches
        );
    }

    public static ReadableGroups newGroups() {
        return IMPL.newGroups();
    }

    public static ReadableGroups newInitializedGroups() {
        return IMPL.newInitializedGroups();
    }

    public static Collector emptyCollector() {
        return IMPL.emptyCollector();
    }

    public static Collector badCollector(OutputStream log, int batchSize) {
        return IMPL.badCollector(log, batchSize);
    }

    public static Estimates knownEstimates(
        long numberOfNodes,
        long numberOfRelationships,
        long numberOfNodeProperties,
        long numberOfRelationshipProperties,
        long sizeOfNodeProperties,
        long sizeOfRelationshipProperties,
        long numberOfNodeLabels
    ) {
        return IMPL.knownEstimates(
            numberOfNodes,
            numberOfRelationships,
            numberOfNodeProperties,
            numberOfRelationshipProperties,
            sizeOfNodeProperties,
            sizeOfRelationshipProperties,
            numberOfNodeLabels
        );
    }

    private static final class BlockFormat {
        private static final BlockFormat INSTANCE = new BlockFormat();

        private org.neo4j.internal.id.IdType nodeType = null;
        private org.neo4j.internal.id.IdType dynamicNodeType = null;

        BlockFormat() {
            try {
                var blockIdType = Class.forName("com.neo4j.internal.blockformat.BlockIdType");
                var blockTypes = Objects.requireNonNull(blockIdType.getEnumConstants());
                for (Object blockType : blockTypes) {
                    var type = (Enum<?>) blockType;
                    switch (type.name()) {
                        case "NODE" -> this.nodeType = (org.neo4j.internal.id.IdType) type;
                        case "DYNAMIC_NODE" -> this.dynamicNodeType = (org.neo4j.internal.id.IdType) type;
                    }
                }
            } catch (ClassNotFoundException | NullPointerException | ClassCastException ignored) {
            }
        }
    }

    public static String versionLongToString(long storeVersion) {
        // copied from org.neo4j.kernel.impl.store.LegacyMetadataHandler.versionLongToString which is private
        if (storeVersion == -1) {
            return "Unknown";
        }
        var bits = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(storeVersion).rewind();
        int length = bits.get() & 0xFF;
        if (length == 0 || length > 7) {
            throw new IllegalArgumentException(
                format(
                    Locale.ENGLISH,
                    "The read version string length %d is not proper.",
                    length
                )
            );
        }
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = (char) (bits.get() & 0xFF);
        }
        return new String(result);
    }

    public static TestLog testLog() {
        return new TestLogImpl();
    }

    @SuppressForbidden(reason = "This is the compat specific use")
    public static Log getUserLog(LogService logService, Class<?> loggingClass) {
        return logService.getUserLog(loggingClass);
    }

    @SuppressForbidden(reason = "This is the compat specific use")
    public static Log getInternalLog(LogService logService, Class<?> loggingClass) {
        return logService.getInternalLog(loggingClass);
    }

    public static NodeValue nodeValue(long id, TextArray labels, MapValue properties) {
        return VirtualValues.nodeValue(id, String.valueOf(id), labels, properties);
    }

    public static Relationship virtualRelationship(long id, Node startNode, Node endNode, RelationshipType type) {
        return new VirtualRelationshipImpl(id, startNode, endNode, type);
    }

    public static GdsDatabaseManagementServiceBuilder databaseManagementServiceBuilder(Path storeDir) {
        return new GdsDatabaseManagementServiceBuilderImpl(storeDir);
    }

    public static String defaultDatabaseFormatSetting() {
        return migratedDefaultDatabaseFormatSetting().orElseGet(GraphDatabaseSettings.db_format::defaultValue);
    }

    private static Optional<String> migratedDefaultDatabaseFormatSetting() {
        try {
            Class<?> cls = Class.forName("com.neo4j.configuration.DefaultDbFormatSettingMigrator");
            Object migrator = cls.getDeclaredConstructor().newInstance();
            var migrateMethod = cls.getDeclaredMethod("migrate", Map.class, Map.class, InternalLog.class);
            var defaultValues = new HashMap<String, String>();
            migrateMethod.invoke(migrator, Map.<String, String>of(), defaultValues, null);
            return Optional.ofNullable(defaultValues.get(GraphDatabaseSettings.db_format.name()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void configureDatabaseFormat(Config.Builder configBuilder, String recordFormat) {
        var databaseRecordFormat = recordFormat.toLowerCase(Locale.ENGLISH);
        configBuilder.set(GraphDatabaseSettings.db_format, databaseRecordFormat);
    }

    public static DatabaseLayout databaseLayout(Config config, String databaseName) {
        var neo4jLayout = neo4jLayout(config);
        var storageEngineFactory = StorageEngineFactory.selectStorageEngine(config);
        return storageEngineFactory.databaseLayout(neo4jLayout, databaseName);
    }

    public static Stream<DatabaseLayout> allAvailableDatabaseLayouts(Config config, String databaseName) {
        var neo4jLayout = neo4jLayout(config);
        return StorageEngineFactory.allAvailableStorageEngines().stream()
            .map(engine -> engine.databaseLayout(neo4jLayout, databaseName));
    }

    @SuppressForbidden(reason = "This is the compat specific use")
    public static Neo4jLayout neo4jLayout(Config config) {
        return Neo4jLayout.of(config);
    }

    public static HostnamePort getLocalBoltAddress(ConnectorPortRegister connectorPortRegister) {
        return connectorPortRegister.getLocalAddress(ConnectorType.BOLT);
    }

    @SuppressForbidden(reason = "This is the compat specific use")
    public static SslPolicyLoader createSllPolicyLoader(
        FileSystemAbstraction fileSystem,
        Config config,
        LogService logService
    ) {
        return SslPolicyLoader.create(fileSystem, config, logService.getInternalLogProvider());
    }

    @SuppressForbidden(reason = "This is the compat API")
    public static CallableProcedure callableProcedure(CompatCallableProcedure procedure) {
        return new CallableProcedureImpl(procedure);
    }

    public static long transactionId(KernelTransactionHandle kernelTransactionHandle) {
        return kernelTransactionHandle.getTransactionSequenceNumber();
    }

    public static long transactionId(KernelTransaction kernelTransaction) {
        return kernelTransaction.getTransactionSequenceNumber();
    }

    public static void reserveNeo4jIds(IdGeneratorFactory generatorFactory, int size, CursorContext cursorContext) {
        var idGenerator = InternalReadOps.findValidIdGeneratorsStream(
                generatorFactory,
                RecordIdType.NODE,
                BlockFormat.INSTANCE.nodeType,
                BlockFormat.INSTANCE.dynamicNodeType
            )
            .findFirst().orElseThrow(InternalReadOps::unsupportedStoreFormatException);

        idGenerator.nextConsecutiveIdRange(size, false, cursorContext);
    }

    public static TransactionalContext newQueryContext(
        TransactionalContextFactory contextFactory,
        InternalTransaction tx,
        String queryText,
        MapValue queryParameters
    ) {
        return contextFactory.newContext(tx, queryText, queryParameters, QueryExecutionConfiguration.DEFAULT_CONFIG);
    }


    public static void registerCloseableResource(
        org.neo4j.kernel.api.KernelTransaction transaction,
        AutoCloseable autoCloseable
    ) {
        transaction.resourceMonitor().registerCloseableResource(autoCloseable);
    }

    private Neo4jProxy() {
        throw new UnsupportedOperationException("No instances");
    }
}
