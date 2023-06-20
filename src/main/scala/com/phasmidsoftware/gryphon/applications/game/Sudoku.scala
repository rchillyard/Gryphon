/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.game

import com.phasmidsoftware.gryphon.games.{Game, Positions}


case class SudokuSquare(squares: Seq[Option[Int]]) extends Positions[Option[Int]](squares) {
    def isComplete: Boolean = false

}

case class SudokuPlayer()

case class Sudoku(board: SudokuSquare) extends Game[Option[Int], SudokuSquare, SudokuPlayer] {

    def players: Seq[SudokuPlayer] = Seq(SudokuPlayer())

    /**
     * If the result of invoking goal exists, then the game is over.
     * If there is a winner, then the optional value is Some(winner).
     * Otherwise, the game is a draw (stalemate) and the result is Some(None).
     *
     * @param board the game board.
     * @return an Option of Option[Player]
     */
    def goal(board: SudokuSquare): Option[Option[SudokuPlayer]] =
        if (board.isComplete) Some(Some(players.head))
        else None
}
