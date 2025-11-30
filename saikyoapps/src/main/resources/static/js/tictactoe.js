// tictactoe.js
// シンプルなクライアント実装。0=空, 1=先手(X), 2=後手(O)

(function () {
  const boardElem = document.getElementById('board');
  const cells = Array.from(document.querySelectorAll('.cell'));
  const statusElem = document.getElementById('currentPlayer');
  const resultElem = document.getElementById('result');
  const resetBtn = document.getElementById('resetBtn');

  let board = [
    [0, 0, 0],
    [0, 0, 0],
    [0, 0, 0]
  ];
  let currentPlayer = 1; // 1 = X, 2 = O
  let gameOver = false;

  function render() {
    cells.forEach(cell => {
      const row = parseInt(cell.getAttribute('data-row'));
      const col = parseInt(cell.getAttribute('data-col'));
      const val = board[row][col];
      cell.textContent = val === 1 ? 'X' : val === 2 ? 'O' : '';
      if (val !== 0 || gameOver) {
        cell.classList.add('disabled');
      } else {
        cell.classList.remove('disabled');
      }
    });
    statusElem.textContent = currentPlayer === 1 ? '先手 (X)' : '後手 (O)';
  }

  function checkWinner() {
    // 横、縦、斜めのチェック
    const lines = [
      // rows
      [[0,0],[0,1],[0,2]],
      [[1,0],[1,1],[1,2]],
      [[2,0],[2,1],[2,2]],
      // cols
      [[0,0],[1,0],[2,0]],
      [[0,1],[1,1],[2,1]],
      [[0,2],[1,2],[2,2]],
      // diags
      [[0,0],[1,1],[2,2]],
      [[0,2],[1,1],[2,0]]
    ];

    for (const line of lines) {
      const [a,b,c] = line;
      const v1 = board[a[0]][a[1]];
      if (v1 !== 0 && v1 === board[b[0]][b[1]] && v1 === board[c[0]][c[1]]) {
        return v1; // 1 or 2
      }
    }

    // 引き分けチェック
    if (board.flat().every(v => v !== 0)) {
      return 3; // draw
    }

    return 0; // no winner
  }

  function onCellClick(e) {
    if (gameOver) return;
    const cell = e.currentTarget;
    const row = parseInt(cell.getAttribute('data-row'));
    const col = parseInt(cell.getAttribute('data-col'));
    if (board[row][col] !== 0) return;

    board[row][col] = currentPlayer;
    const winner = checkWinner();
    if (winner === 1) {
      resultElem.textContent = '先手 (X) の勝ち！';
      gameOver = true;
    } else if (winner === 2) {
      resultElem.textContent = '後手 (O) の勝ち！';
      gameOver = true;
    } else if (winner === 3) {
      resultElem.textContent = '引き分けです。';
      gameOver = true;
    } else {
      currentPlayer = currentPlayer === 1 ? 2 : 1;
    }
    render();
  }

  function resetGame() {
    board = [
      [0,0,0],
      [0,0,0],
      [0,0,0]
    ];
    currentPlayer = 1;
    gameOver = false;
    resultElem.textContent = '';
    render();
  }

  cells.forEach(cell => cell.addEventListener('click', onCellClick));
  resetBtn.addEventListener('click', resetGame);

  // 初期描画
  render();
})();
