package org.testaco

import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.IDataSet
import org.dbunit.operation.DatabaseOperation

interface IDatabaseTester {

    @get:Throws(Exception::class)
    val connection: IDatabaseConnection?
    /**
     * Returns the test dataset.
     */
    /**
     * Sets the test dataset to use.
     */
    var dataSet: IDataSet?
    /**
     * Gets the DatabaseOperation to call when starting the test.
     */
    /**
     * Sets the DatabaseOperation to call when starting the test.
     */
    var setUpOperation: DatabaseOperation?
    /**
     * Gets the DatabaseOperation to call when ending the test.
     */
    /**
     * Sets the DatabaseOperation to call when ending the test.
     */
    var tearDownOperation: DatabaseOperation?

    /**
     * TestCases must call this method inside setUp()
     */
    @Throws(Exception::class)
    fun onSetup()

    /**
     * TestCases must call this method inside tearDown()
     */
    @Throws(Exception::class)
    fun onTearDown()

    /**
     * @param operationListener
     * The operation listener that is invoked on specific events in
     * the [IDatabaseTester].
     * @since 2.4.4
     */
    fun setOperationListener(operationListener: IOperationListener?)
}