from sudoku_lib import SudokuSolver

display = widget.getDisplayModel()

def readBoard():
    board = [[0] * 9 for x in range(9)]
    for row in range(9):
        for col in range(9):
            value = PVUtil.getString(pvs[calcPVIndex(row, col)-1])
            if value == " " or value == "#":
                value = 0
            else:
                value = int(value)
            board[row][col] = value
    return board

def writeBoard(board):
    for row in range(9):
        for col in range(9):
            pv = pvs[1+row*9+col]
            cell = getWidget(row, col)
            #only write into non-preset cells
            value = board[row][col]
            strFromPV = PVUtil.getString(pv)
            if value > 0 and (str(value) != strFromPV or strFromPV == "#"):
                if value > 9:
                    pv.write("#")
                else:
                    pv.write(str(value))
            #else: deliberately ignored
def clearBoard():
    for index in range (1,82):
        pv = pvs[index]
        if PVUtil.getDouble(pv) != -1:
            pv.write(0)

#------ begin execution ------
if int(PVUtil.getDouble(pvs[0])) == 1:
    board = readBoard()
    solver = SudokuSolver(board)
    board = solver.solve()
    writeBoard(board)
