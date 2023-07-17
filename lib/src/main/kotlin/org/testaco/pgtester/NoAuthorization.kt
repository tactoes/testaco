package org.testaco.pgtester

/**
 * Marker annotation to explicitly mark service methods that are publically accessible. Useful to help people who review your
 * code figure out you forgot something, or to make a test that checks that all service classes have had authorization
 * thought about. Meant to work in conjunction with @PreAuthorize from spring security.
 */
annotation class NoAuthorization
