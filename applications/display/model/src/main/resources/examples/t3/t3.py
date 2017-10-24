# -*- coding: UTF-8 -*-
"""Tic Tac Toe

   Not the world's best implementation,
   but that's partially the point:
   Scripts run in a background thread,
   so they may perform computations.
   
   @author Kay Kasemir
"""

class T3:
    """Min/max type search for best move"""
    def __init__(self, board = "         "):
        self.board = list(board)

    def isDone(self):
        for pos in range(9):
            if self.board[pos] == ' ':
                return False
        return True

    def determineWinner(self):
        for i in range(3):
            # Check horizontal rows
            row = i*3
            if self.board[row] != ' ' and \
              (self.board[row] == self.board[row+1] == self.board[row+2]):
                return self.board[row]
            # Check vertical columns
            col = i
            if self.board[col] != ' ' and \
              (self.board[col] == self.board[col+3] == self.board[col+6]):
                return self.board[col]
        # Check diagonals
        if self.board[4] != ' ' and \
          (self.board[0] == self.board[4] == self.board[8]):
            return self.board[4]
        if self.board[4] != ' ' and \
          (self.board[2] == self.board[4] == self.board[6]):
            return self.board[4]
        return None
    
    def determineMaxScore(self):
        winner = self.determineWinner()
        if winner == 'x':
            return ( +1, None)
        elif winner == 'o':
            return ( -1, None )
        elif self.isDone():
            return ( 0, None )
        else:
            best_score = -1
            best_pos = None
            for pos in range(9):
                if self.board[pos] != ' ':
                    continue
                self.board[pos] = 'x'
                ( score, nop ) = self.determineMinScore()
                self.board[pos] = ' '
                if score >= best_score:
                    best_score = score
                    best_pos = pos
            return ( best_score, best_pos )
        
    def determineMinScore(self):
        winner = self.determineWinner()
        if winner == 'x':
            return ( +1, None)
        elif winner == 'o':
            return ( -1, None )
        elif self.isDone():
            return ( 0, None )
        else:
            best_score = +1
            best_pos = None
            for pos in range(9):
                if self.board[pos] != ' ':
                    continue
                self.board[pos] = 'o'
                ( score, nop ) = self.determineMaxScore()
                self.board[pos] = ' '
                if score <= best_score:
                    best_score = score
                    best_pos = pos
            return ( best_score, best_pos )

    def makeMove(self, player):
        if player == 'x':
            ( best_score, best_pos ) = self.determineMaxScore()
        else:
            ( best_score, best_pos ) = self.determineMinScore()
        return best_pos
        
    def __str__(self):
        return ("|".join(self.board[0:3]) + "  0|1|2\n" +
                "-+-+-  -+-+-\n" +
                "|".join(self.board[3:6]) + "  3|4|5\n" + 
                "-+-+-  -+-+-\n" +
                "|".join(self.board[6:9]) + "  6|7|8")
        

def twoPlayerSuggestion():
    t3 = T3()
    print(t3)
    winner = t3.determineWinner()
    player = 'o'
    while not t3.isDone() and winner is None:
        test = T3(t3.board)
        pos = test.makeMove(player)
        print("Setting " + str(pos) + " would be best for " + player)
        i = int(input(player + "? "))
        t3.board[i] = player
        player = 'x' if player == 'o' else 'o'
        print(t3)
        winner = t3.determineWinner()
    print("Winner: " + str(winner))

def hmi():
    print("Tic-Tac-Toe\n")
    print("===========\n")
    print("You are 'x', I am 'o'\n")
    t3 = T3()
    print(t3)
    winner = t3.determineWinner()
    while not t3.isDone() and winner is None:
        while True:
            pos = int(input("Your move: "))
            if t3.board[pos] != ' ':
                print("There's already " + t3.board[pos] + ", try again!")
            else:
                break
        t3.board[pos] = 'x'
        print(t3)
        winner = t3.determineWinner()
        if t3.isDone():
            break

        test = T3(t3.board)
        pos = test.makeMove('o')
        print("I set " + str(pos))
        t3.board[pos] = 'o'
        print(t3)
        winner = t3.determineWinner()
    if winner == 'x':
        print("You're the Weener!")
    elif winner == 'o':
        print("You snooze, you loose!")
    else:
        print("We're equals!!")

if __name__ == "__main__":
    hmi()
