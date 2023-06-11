package org.testaco

import org.dbunit.IDatabaseTester
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.IDataSet
import org.dbunit.operation.DatabaseOperation
import org.slf4j.Logger
import org.slf4j.LoggerFactory


abstract class DatabaseTestCase {
    private var tester: IDatabaseTester? = null
    protected var operationListener: IOperationListener? = null
        /**
         * @return The [IOperationListener] to be used by the [IDatabaseTester].
         * @since 2.4.4
         */
        protected get() {
            logger.debug("getOperationListener() - start")
            if (field == null) {
                field = object : DefaultOperationListener() {
                    fun connectionRetrieved(connection: IDatabaseConnection) {
                        super.connectionRetrieved(connection)
                        // When a new connection has been created then invoke the setUp method
                        // so that user defined DatabaseConfig parameters can be set.
                        setUpDatabaseConfig(connection.getConfig())
                    }
                }
            }
            return field
        }
        private set

    constructor()
    constructor(name: String?) : super(name)

    @get:Throws(Exception::class)
    protected abstract val connection: IDatabaseConnection

    @get:Throws(Exception::class)
    protected abstract val dataSet: IDataSet?

    /**
     * Creates a IDatabaseTester for this testCase.<br></br>
     *
     * A [DefaultDatabaseTester] is used by default.
     * @throws Exception
     */
    @Throws(Exception::class)
    protected fun newDatabaseTester(): IDatabaseTester {
        logger.debug("newDatabaseTester() - start")
        val connection: IDatabaseConnection = connection
        operationListener.connectionRetrieved(connection)
        return DefaultDatabaseTester(connection)
    }

    /**
     * Designed to be overridden by subclasses in order to set additional configuration
     * parameters for the [IDatabaseConnection].
     * @param config The settings of the current [IDatabaseConnection] to be configured
     */
    protected fun setUpDatabaseConfig(config: DatabaseConfig?) {
        // Designed to be overridden.
    }

    @get:Throws(Exception::class)
    protected val databaseTester: IDatabaseTester?
        /**
         * Gets the IDatabaseTester for this testCase.<br></br>
         * If the IDatabaseTester is not set yet, this method calls
         * newDatabaseTester() to obtain a new instance.
         * @throws Exception
         */
        protected get() {
            if (tester == null) {
                tester = newDatabaseTester()
            }
            return tester
        }

    /**
     * Close the specified connection. Override this method of you want to
     * keep your connection alive between tests.
     */
    @Deprecated("since 2.4.4 define a user defined {@link #getOperationListener()} in advance")
    @Throws(Exception::class)
    protected fun closeConnection(connection: IDatabaseConnection?) {
        logger.debug("closeConnection(connection={}) - start", connection)
        assertNotNull("DatabaseTester is not set", databaseTester)
        databaseTester.closeConnection(connection)
    }

    @get:Throws(Exception::class)
    protected val setUpOperation: DatabaseOperation
        /**
         * Returns the database operation executed in test setup.
         */
        protected get() = DatabaseOperation.CLEAN_INSERT

    @get:Throws(Exception::class)
    protected val tearDownOperation: DatabaseOperation
        /**
         * Returns the database operation executed in test cleanup.
         */
        protected get() = DatabaseOperation.NONE

    ////////////////////////////////////////////////////////////////////////////
    // TestCase class
    @Throws(Exception::class)
    protected override fun setUp() {
        logger.debug("setUp() - start")
        super.setUp()
        val databaseTester: IDatabaseTester? = databaseTester
        assertNotNull("DatabaseTester is not set", databaseTester)
        databaseTester.setSetUpOperation(setUpOperation)
        databaseTester.setDataSet(dataSet)
        databaseTester.setOperationListener(operationListener)
        databaseTester.onSetup()
    }

    @Throws(Exception::class)
    protected override fun tearDown() {
        logger.debug("tearDown() - start")
        try {
            val databaseTester: IDatabaseTester? = databaseTester
            assertNotNull("DatabaseTester is not set", databaseTester)
            databaseTester.setTearDownOperation(tearDownOperation)
            databaseTester.setDataSet(dataSet)
            databaseTester.setOperationListener(operationListener)
            databaseTester.onTearDown()
        } finally {
            tester = null
            super.tearDown()
        }
    }

    companion object {
        /**
         * Logger for this class
         */
        private val logger: Logger = LoggerFactory.getLogger(DatabaseTestCase::class.java)
    }
}