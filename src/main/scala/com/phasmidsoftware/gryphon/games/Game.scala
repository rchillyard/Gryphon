/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.games

trait Game[Position, Board <: Positions[Position], Player] {

    def board: Board

    def players: Seq[Player]

    /**
     * If the result of invoking goal exists, then the game is over.
     * If there is a winner, then the optional value is Some(winner).
     * Otherwise, the game is a draw (stalemate) and the result is Some(None).
     *
     * @param board the game board.
     * @return an Option of Option[Player]
     */
    def goal(board: Board): Option[Option[Player]]
}
