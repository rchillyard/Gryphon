/*
 * Copyright (c) 2026. Phasmid Software
 */

package com.phasmidsoftware.gryphon.util

/**
 * Deprecated alias for [[com.phasmidsoftware.gryphon.core.GraphException]].
 *
 * Gryphon had two identical `GraphException` case classes, one here and one in `core`. They were unrelated
 * types, so catching one did not catch the other. `core` is now the only one, and this alias exists so that
 * code written against 1.6.0 or earlier still compiles.
 */
@deprecated("Use com.phasmidsoftware.gryphon.core.GraphException", since = "1.6.1")
type GraphException = com.phasmidsoftware.gryphon.core.GraphException

/**
 * Deprecated alias for the [[com.phasmidsoftware.gryphon.core.GraphException]] companion object,
 * so that `GraphException("...")` and pattern matching still work through the old name.
 */
@deprecated("Use com.phasmidsoftware.gryphon.core.GraphException", since = "1.6.1")
val GraphException: com.phasmidsoftware.gryphon.core.GraphException.type = com.phasmidsoftware.gryphon.core.GraphException
