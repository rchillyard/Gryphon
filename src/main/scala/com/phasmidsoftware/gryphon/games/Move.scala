/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.games

/**
 * Trait to model a move in a game.
 * Normally, a player transfers/receives some sort of token to/from the board, and/or receives/transfers a token back.
 * The net effect of this might be that the token has moved on the board.
 *
 * @tparam Board  the board type.
 * @tparam Player the player type.
 */
trait Move[Board, Player] extends ((Board, Player) => (Board, Player))


