package com.bale.tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.bale.tetris.Shape.Tetrominoes;
import com.bale.tetris.sound.Sound;

public class Board extends JPanel implements ActionListener {
	final int BoardWidth = 10;
	final int BoardHeight = 22;
	Timer timer;
	boolean isFallingFinished = false;
	boolean isStarted = false;
	boolean isPaused = false;
	String gameOverMsg = "PRESS [ESC] TO RESTART.";
	int numLinesRemoved = 0;
	int curX = 0;
	int curY = 0;
	JLabel statusbar;
	Shape curPiece;
	Tetrominoes[] board;
	int gameSpeed = 400;
	Tetris parent;

	public Board(Tetris parent) {
		this.parent = parent;
		setFocusable(true);
		curPiece = new Shape();
		timer = new Timer(gameSpeed, this);
		timer.start();

		statusbar = parent.getStatusBar();
		board = new Tetrominoes[BoardWidth * BoardHeight];
		addKeyListener(new TAdapter());
		clearBoard();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (isFallingFinished) {
			isFallingFinished = false;
			newPiece();
		} else {
			oneLineDown();
		}
	}
	
	public void restart(Tetris parent) {
		Sound.test.play();
		isStarted = true;
		isFallingFinished = false;
		numLinesRemoved = 0;
		curPiece = new Shape();
		curPiece.setRandomShape();
		statusbar = parent.getStatusBar();
		board = new Tetrominoes[BoardWidth * BoardHeight];
		clearBoard();
		newPiece();
		repaint();
		gameSpeed = 400;
		timer = new Timer(gameSpeed, this);
		statusbar.setText(String.valueOf(numLinesRemoved));
		timer.start();
	}

	public void start() {
		if (isPaused) {
			return;
		}

		if (isStarted) {
			isFallingFinished = false;
			numLinesRemoved = 0;
			clearBoard();
			newPiece();
			timer.start(); 
		}
	}

	private void pause() {
		if (!isStarted) 
			return;

		Sound.test.play();
		isPaused = !isPaused;
		if (isPaused) {
			timer.stop();
			statusbar.setText("Paused");
		} else {
			timer.start();
			statusbar.setText(String.valueOf(numLinesRemoved));
		}
		repaint();
	}
	
	private void newPiece() {
		curPiece.setRandomShape();
		curX = BoardWidth / 2 + 1;
		curY = BoardHeight - 1 + curPiece.minY();

		if (!tryMove(curPiece, curX, curY) || numLinesRemoved == 50) {
			timer.stop();
			isStarted = false;
			statusbar.setText(gameOverMsg);
		}
	}

	public void paint(Graphics g) {
		if (!isStarted && !statusbar.getText().equals(gameOverMsg)) {
			Font font  = new Font("Tahoma", Font.BOLD, 15);
			FontMetrics metrics = g.getFontMetrics(font);
			g.setFont(font);

			String msg = "TETRIS";
			g.drawString(msg, this.WIDTH / 2 + 60, 150);

			msg = "Press Enter to Play";
			g.drawString(msg, this.WIDTH / 2 + 20, 200);
			
		} else {
			super.paint(g);

			Dimension size = getSize();
			int boardTop = (int) size.getHeight() - BoardHeight * squareHeight();

			for (int i = 0; i < BoardHeight; i++) {
				for (int j = 0; j < BoardWidth; j++) {
					Tetrominoes shape = shapeAt(j, BoardHeight - i - 1);
					if (shape != Tetrominoes.NoShape)
						drawSquare(g, 0 + j * squareWidth(), 
								boardTop + i * squareHeight(), shape);
				}
			}

			if (curPiece.getShape() != Tetrominoes.NoShape) {
				for (int i = 0; i < 4; i++) {
					int x = curX + curPiece.x(i);
					int y = curY - curPiece.y(i);

					drawSquare(g, 0 + x * squareWidth(),
							boardTop + (BoardHeight - y - 1) * squareHeight(),
							curPiece.getShape());
				}
			}
		}
	}

	int squareWidth() {
		return (int)getSize().getWidth() / BoardWidth;
	}

	int squareHeight() {
		return (int)getSize().getHeight() / BoardHeight;
	}

	Tetrominoes shapeAt(int x, int y) {
		return board[x + y * BoardWidth];
	}

	private void dropDown() {
		int newY = curY;
		while (newY > 0) {
			if (!tryMove(curPiece, curX, newY - 1)) 
				break;
			--newY;
		}
		pieceDropped();
	}

	private void oneLineDown() {
		if (!tryMove(curPiece, curX, curY - 1)) {
			pieceDropped();
		}
	}

	private void clearBoard() {
		for (int i = 0; i < BoardHeight * BoardWidth; i++) {
			board[i] = Tetrominoes.NoShape;
		}
	}

	private void pieceDropped() {
		for (int i = 0; i < 4; ++i) {
			int x = curX + curPiece.x(i);
			int y = curY - curPiece.y(i);
			board[x + y * BoardWidth] = curPiece.getShape();
		}

		removeFullLines();

		if (!isFallingFinished) {
			newPiece();
		}
	}

	private boolean tryMove(Shape newPiece, int newX, int newY) {
		for (int i = 0; i < 4; i++) {
			int x = newX + newPiece.x(i);
			int y = newY - newPiece.y(i);

			if (x < 0 || y < 0 || x >= BoardWidth || y >= BoardHeight) 
				return false;

			if (shapeAt(x, y) != Tetrominoes.NoShape) 
				return false;
		}

		curPiece = newPiece;
		curX = newX;
		curY = newY;
		repaint();
		return true;
	}

	private void removeFullLines() {
		int numFullLines = 0;

		for (int y = BoardHeight - 1; y >= 0; y--) {
			boolean lineIsFull = true;

			for (int x = 0; x < BoardWidth; x++) {
				if (shapeAt(x, y) == Tetrominoes.NoShape) {
					lineIsFull = false;
					break;
				}
			}

			if (lineIsFull) {
				if (gameSpeed >= 25) {
					gameSpeed -= 25;
					timer.stop();
					timer = new Timer(gameSpeed, this);
					timer.start();
				}

				numFullLines++;
				for (int k = y; k < BoardHeight - 1; k++) {
					for (int x = 0; x < BoardWidth; x++) 
						board[x + k * BoardWidth] = shapeAt(x, k + 1);
				}
			}
		}
		if (numFullLines > 0) {
			numLinesRemoved += numFullLines;
			statusbar.setText(String.valueOf(numLinesRemoved));
			isFallingFinished = true;
			curPiece.setShape(Tetrominoes.NoShape);
			repaint();
		}
	}

	private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {
		Color colors[] = { new Color(0, 0, 0), new Color(204, 102, 102),
				new Color(102, 204, 102), new Color(102, 102, 204), 
				new Color(204, 204, 102), new Color(204, 102, 204), 
				new Color(102, 204, 204), new Color(218, 170, 0)
		};

		Color color = colors[shape.ordinal()];

		g.setColor(color);
		g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);

		g.setColor(color.brighter());
		g.drawLine(x, y + squareHeight() - 1, x, y);
		g.drawLine(x, y , x + squareWidth() - 1, y);

		g.setColor(color.brighter());
		g.drawLine(x + 1, y + squareHeight() - 1, 
				x + squareWidth() - 1, y + squareHeight() -1);
		g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1,
				x + squareWidth() - 1, y + 1);
	}

	class TAdapter extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			if (curPiece.getShape() == Tetrominoes.NoShape)
				return;

			int keycode = e.getKeyCode();

			if (keycode == 'p' || keycode == 'P') {
				pause();
				return;
			}

			if (isPaused) 
				return;

			switch (keycode) {
			case KeyEvent.VK_LEFT:
				tryMove(curPiece, curX - 1, curY);
				break;
			case KeyEvent.VK_RIGHT:
				tryMove(curPiece, curX + 1, curY);
				break;
			case KeyEvent.VK_DOWN:
				tryMove(curPiece.rotateRight(), curX, curY);
				break;
			case KeyEvent.VK_UP:
				tryMove(curPiece.rotateLeft(), curX, curY);
				break;
			case KeyEvent.VK_SPACE:
				dropDown();
				break;
			case KeyEvent.VK_ENTER:
				Sound.test.play();
				isStarted = true;
				break;
			case KeyEvent.VK_ESCAPE:
				restart(parent);
				break;
			case 'd':
				oneLineDown();
				break;
			case 'D':
				oneLineDown();
				break;
			}
		}
	}
}