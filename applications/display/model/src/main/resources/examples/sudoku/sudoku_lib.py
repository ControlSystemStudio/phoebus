class SudokuSolver:
    def __init__(self, board):
        self.board = [[0] * 9 for x in range(9)] #using [[0]*9]*9 causes an array of nine identical rows
        self.numSolved = 0
        self.poss = [ [x for x in range(1,10)] for i in range(81) ]
        for row in range(9):
            for col in range(9):
                value = board[row][col]
                self.solveCell(row, col, value)
       
    def solveCell(self, row, col, val):
        if self.board[row][col] != 0 or val == 0: return
        self.board[row][col] = val
        self.poss[row*9+col] = [val]
        self.changed = True
        for currRow in range(9):
            if currRow != row:
                self.removeValsFromCell(currRow, col, val)
        for currCol in range(9):
            if currCol != col:
                self.removeValsFromCell(row, currCol, val)
        for currRow in SudokuSolver.calcBlockRange(row):
            for currCol in SudokuSolver.calcBlockRange(col):
                if currRow != row or currCol != col:
                    self.removeValsFromCell(currRow, currCol, val)
        self.numSolved += 1
    def removeValsFromCell(self, row, col, *vals):
        for val in vals:
            if self.poss[row*9+col].count(val) > 0:
                self.poss[row*9+col].remove(val)
                self.changed = True

    def solve(self):
        while self.numSolved < 81 and self.changed:
            self.changed = False
            for x in range(9):
                if not self.isSolvableRow(x) or \
                   not self.isSolvableCol(x) or \
                   not self.isSolvableBlock(x-x%3, x%3*3):
                    return self.board
        return self.board
    
    def isSolvableRow(self,row):
        numFoundOf = [0] * 9
        lastFoundAt = [(-1,-1)] * 9
        for col in range(9):
            if not self.isSolvableCell(row, col, numFoundOf, lastFoundAt):
                return False
        self.scanUnit(numFoundOf, lastFoundAt)
        return True

    def isSolvableCol(self,col):
        numFoundOf = [0] * 9
        lastFoundAt = [(-1,-1)] * 9
        for row in range(9):
            if not self.isSolvableCell(row, col, numFoundOf, lastFoundAt):
                return False
        self.scanUnit(numFoundOf, lastFoundAt)
        return True
    
    def isSolvableBlock(self, rowStart, colStart):
        numFoundOf = [0] * 9
        lastFoundAt = [(-1,-1)] * 9
        for row in range(rowStart, rowStart+3):
            for col in range(colStart, colStart+3):
                if not self.isSolvableCell(row, col, numFoundOf, lastFoundAt):
                    return False
        self.scanUnit(numFoundOf, lastFoundAt)
        return True
    
    def isSolvableCell(self, row, col, numFoundOf, lastFoundAt):
        currPoss = self.poss[row*9+col]
        numPoss = len(currPoss)
        if numPoss==0:
            self.board[row][col] = 10
            return False
        elif numPoss==1:
            value = currPoss[0]
            self.solveCell(row, col, value)
            numFoundOf[value-1] = -1
        else:
            for possVal in currPoss:
                 if numFoundOf[possVal-1] != -1:
                     numFoundOf[possVal-1] += 1
                     lastFoundAt[possVal-1] = (row, col)
        return True

    def scanUnit(self, numFoundOf, lastFoundAt):
        for i in range(9):
            numFound = numFoundOf[i]
            if numFound == 1:
                row = lastFoundAt[i][0]; col = lastFoundAt[i][1]
                self.solveCell(row, col, i+1)

    @staticmethod
    def calcBlockRange(val):
        return range(val-val%3, val-val%3+3)

if __name__ == "__main__":
    # Standalone demo code
    print("Sudoku Demo")
    
    board = [ [0,2,0, 0,0,4, 3,0,0],
              [9,0,0, 0,2,0, 0,0,8],
              [0,0,0, 6,0,9, 0,5,0],
              
              [0,0,0, 0,0,0, 0,0,1],
              [0,7,2, 5,0,3, 6,8,0],
              [6,0,0, 0,0,0, 0,0,0],

              [0,8,0, 2,0,5, 0,0,0],
              [1,0,0, 0,9,0, 0,0,3],
              [0,0,9, 8,0,0, 0,6,0] ]
    solver = SudokuSolver(board)
    
    def printBoard(solver):
        for x in range(9):
            print("  %d %d %d  %d %d %d  %d %d %d" % (solver.board[x][0],solver.board[x][1],solver.board[x][2],
                                                  solver.board[x][3],solver.board[x][4],solver.board[x][5],
                                                  solver.board[x][6],solver.board[x][7],solver.board[x][8]))
            if x%3==2:
                print("")

    print("blank:")
    printBoard(solver)

    board = solver.solve()
    
    print("solved:")
    printBoard(solver)
    
    unsolvable = [ [5,2,0, 0,0,4, 3,0,0],
                   [9,0,0, 0,2,0, 0,0,8],
                   [0,0,0, 6,0,9, 0,5,0],
                 
                   [0,0,0, 0,0,0, 0,0,1],
                   [0,7,2, 5,0,3, 6,8,0],
                   [6,0,0, 0,0,0, 0,0,0],
     
                   [0,8,0, 2,0,5, 0,0,0],
                   [1,0,0, 0,9,0, 0,0,3],
                   [0,0,9, 8,0,0, 0,6,0] ]
    
    solver = SudokuSolver(unsolvable)

    print("unsolvable:")
    printBoard(solver)

    board = solver.solve()
    
    print("unsolved:")
    printBoard(solver)
    
