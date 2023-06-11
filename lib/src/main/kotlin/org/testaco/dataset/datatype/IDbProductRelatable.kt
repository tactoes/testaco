package org.testaco.dataset.datatype

/**
 * Reports what database products this object relates to.
 * Typically implemented by an `IDaatTypeFactory`.
 */
interface IDbProductRelatable {
    val validDbProducts: Collection<String>
}
