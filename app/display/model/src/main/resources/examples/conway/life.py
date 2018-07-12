"""Conway's Game of Life

   Text-based demo of the game logic
   
   @author Kay Kasemir
"""

class GameOfLife(object):
    def __init__(self, width=30, height=20):
        self.piece = ( '.', 'X' )
        self.width = width
        self.height = height
        
        # Note:  [ [ 0 ] * self.width ] * self.height
        # would result in all 'rows' referencing the same
        # array instance
        self.map = [ [ 0 ] * self.width for r in range(self.height) ]        

        # Toggle
        self.map[2][10] = 1
        self.map[3][10] = 1
        self.map[4][10] = 1

        # Glider
        self.map[1][1] = 1
        self.map[2][2] = 1

        self.map[0][3] = 1
        self.map[1][3] = 1
        self.map[2][3] = 1

    def neighbors(self, row, col):
        n = 0
        for r in ( -1, 0, +1):
            for c in ( -1, 0, +1):
                if ((r != 0 or c != 0) and
                    0 <= row+r < self.height and
                    0 <= col+c < self.width  and
                    self.map[row+r][col+c] > 0):
                    n += 1
        return n

    def evolve(self):
        self.next = [ [ 0 ] * self.width for r in range(self.height)]
        for row in range(self.height):
            for col in range(self.width):
                n = self.neighbors(row, col)
                c = self.map[row][col]
                if ((c > 0  and  2 <= n <= 3) or
                    (c == 0  and  n == 3)):
                    self.next[row][col] = 1
                else:
                    self.next[row][col] = 0
        self.map = self.next
        

    def __repr__(self):
        return "\n".join(
                         [ " ".join( [ self.piece[col] for col in row ] ) for row in self.map ]
                        )


if __name__ == "__main__":
    gol = GameOfLife(12, 5)
    for gen in range(5):
        print(gol)
        print("--------")
        gol.evolve()
