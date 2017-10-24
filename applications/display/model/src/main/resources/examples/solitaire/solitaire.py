"""Solitaire solution finder and re-play

   Not the world's best implementation,
   but that's partially the point:
   Scripts run in a background thread,
   so they may perform computations,
   and at the same time they can access the display model.
   
   @author Kay Kasemir
"""

# Game board model
board = [ [ ' ', ' ', 'o', 'o', 'o', ' ', ' ', ],
          [ ' ', ' ', 'o', 'o', 'o', ' ', ' ', ],
          [ 'o', 'o', 'o', 'o', 'o', 'o', 'o', ],
          [ 'o', 'o', 'o', '.', 'o', 'o', 'o', ],
          [ 'o', 'o', 'o', 'o', 'o', 'o', 'o', ],
          [ ' ', ' ', 'o', 'o', 'o', ' ', ' ', ],
          [ ' ', ' ', 'o', 'o', 'o', ' ', ' ', ]
        ]
# width == height == 'size'
size = len(board)

# ------------ Brute-force backtracking solver -------

# def show():
#     for row in board:
#         print "".join(c for c in row)

def isDone():
    """Check if there is one piece in center,
       and that is the only piece left on the board"""
    if board[size/2][size/2] != 'o':
        return False
    # Would be more efficient to track piece count
    # when performing moves instead of counting
    # each time isDone() is called, but good enough
    # for demo
    count = 0
    for row in board:
        for piece in row:
            if piece == 'o':
                count += 1
                if count > 1:
                    return False
    return count == 1

# Assemble moves as [ row, col, row direction, col. direction ]
# [ 1, 2, -1, 0 ] means move piece in row 1, col 2 'up'
solution = []

def check(r, c, dr, dc):
    """Does move of piece at r,c in direction dr, dc
       lead to solution?
       If yes, that move is added to solution.
    """
    r1 = r + dr
    c1 = c + dc
    r2 = r1 + dr
    c2 = c1 + dc
    if (0 <= r2 < size and
        0 <= c2 < size and
        board[r1][c1] == 'o' and
        board[r2][c2] == '.'):
        # Perform that move
        board[r][c] = board[r1][c1] = '.'
        board[r2][c2] = 'o'
        if solve():
            solution.append([r, c, dr, dc])
            return True
        # Un-do that move to backtrack
        board[r][c] = board[r1][c1] = 'o'
        board[r2][c2] = '.'

def solve():
    if isDone():
        return True
   
    for r in range(size):
        for c in range(size):
            if board[r][c] != 'o':
                continue
            # Move 'right'?
            if check(r, c, 0, +1):
                return True
            # Move 'left'?
            if check(r, c, 0, -1):
                return True
            # Move 'down'?
            if check(r, c, +1, 0):
                return True
            # Move 'up'?
            if check(r, c, -1, 0):
                return True
    return False


# ------------ Visualization via display model -------

from org.csstudio.display.builder.model import WidgetFactory
from org.csstudio.display.builder.model.properties import WidgetColor
from time import time, sleep

TOP = 100
SIZE = 50
GAP = 5
# Board made of Ellipse widgets
pieces = None
# Label widget
info = None

def createWidget(type):
	return WidgetFactory.getInstance().getWidgetDescriptor(type).createWidget();

def createPiece(x, y, set):
    piece = createWidget("ellipse");
    piece.setPropertyValue("x", x)
    piece.setPropertyValue("y", y)
    piece.setPropertyValue("width", SIZE)
    piece.setPropertyValue("height", SIZE)
    piece.setPropertyValue("transparent", not set)
    return piece

def createPieces():
    global pieces, info
    display = widget.getDisplayModel()
    pieces = [ [ None for c in range(size) ] for r in range(size) ]
    for r in range(size):
        for c in range(size):
            p = board[r][c]
            if p != ' ':
                piece = createPiece(c * (SIZE+GAP),
                                    TOP + r * (SIZE+GAP),
                                    p == 'o')
                display.runtimeChildren().addChild(piece)
                pieces[r][c] = piece
    info = createWidget("label")
    info.setPropertyValue("y", TOP + size * (SIZE+GAP))
    info.setPropertyValue("width", size * SIZE)
    info.setPropertyValue("text", "Solving...")
    display.runtimeChildren().addChild(info)
   
createPieces()
start = time()
if not solve():
    raise Exception("No solution!")
end = time()
info.setPropertyValue("text", "Solved in %g sec" % (end - start))
# Solution is populated last-move-first, change into move 1, move 2, ..
solution.reverse()

mark = WidgetColor(255, 0, 0)

def move(r, c, dr, dc):
    # Highlight jumping and jumped piece
    col = pieces[r][c].getPropertyValue("background_color")
    pieces[r][c].setPropertyValue("background_color", mark)
    pieces[r+dr][c+dc].setPropertyValue("background_color", mark)
    sleep(0.5)
    
    # Restore colors
    pieces[r][c].setPropertyValue("background_color", col)
    pieces[r+dr][c+dc].setPropertyValue("background_color", col)

    # Remove/move pieces
    pieces[r][c].setPropertyValue("transparent", True)
    pieces[r+dr][c+dc].setPropertyValue("transparent", True)
    pieces[r+2*dr][c+2*dc].setPropertyValue("transparent", False)
    sleep(0.2)

while True:    
    # Replay moves of the solution
    for m in solution:
        move(*m)
    sleep(2.0)
    
    # Restore board
    for r in range(size):
        for c in range(size):
            p = board[r][c]
            if p != ' ':
                pieces[r][c].setPropertyValue("transparent", (r == size/2 and c == size/2))
    sleep(1.0)
