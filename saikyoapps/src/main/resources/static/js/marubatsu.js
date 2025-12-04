// marubatsu.js
(function () {
  const matchId = new URLSearchParams(window.location.search).get('matchId');
  const boardElem = document.getElementById('board');
  const cells = Array.from(document.querySelectorAll('.cell'));
  const statusElem = document.getElementById('currentPlayer');
  const resultElem = document.getElementById('result');

  let pollInterval = 1500;
  let pollTimer = null;

  async function fetchState() {
    if (!matchId) return;
    try {
      const res = await fetch('/match/state?matchId=' + encodeURIComponent(matchId), { cache: 'no-store' });
      const j = await res.json();
      if (!j) return;
      // j.board is array of 9 ints
      const board = j.board || [];
      // render
      cells.forEach(cell => {
        const r = parseInt(cell.getAttribute('data-row'));
        const c = parseInt(cell.getAttribute('data-col'));
        const idx = r * 3 + c;
        const v = board[idx] || 0;
        cell.textContent = v === 1 ? 'X' : v === 2 ? 'O' : '';
        if (v !== 0 || j.status === 'finished') {
          cell.classList.add('disabled');
        } else {
          cell.classList.remove('disabled');
        }
      });

      if (j.status === 'finished') {
        if (j.winner) {
          resultElem.textContent = j.winner + ' の勝ち！';
        } else {
          resultElem.textContent = '引き分けです。';
        }
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        return;
      }

      // 表示ターン
      const turn = j.turn;
      if (turn === j.player1) {
        statusElem.textContent = '先手 (X): ' + j.player1;
      } else {
        statusElem.textContent = '後手 (O): ' + j.player2;
      }

    } catch (e) {
      console.error(e);
    }
  }

  async function sendMove(pos) {
    if (!matchId) return;
    try {
      const tokenEl = document.querySelector('input[name="_csrf"]');
      const token = tokenEl ? tokenEl.value : null;
      const resp = await fetch('/match/move', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': token
        },
        body: JSON.stringify({ matchId: matchId, pos: pos })
      });
      const j = await resp.json();
      if (j.error) {
        alert('Error: ' + j.error);
      }
      // state は次の poll で取得される
    } catch (e) {
      console.error(e);
    }
  }

  function onCellClick(e) {
    const cell = e.currentTarget;
    const r = parseInt(cell.getAttribute('data-row'));
    const c = parseInt(cell.getAttribute('data-col'));
    const idx = r * 3 + c;
    // optimistically disable click until poll updates
    cell.classList.add('disabled');
    sendMove(idx);
  }

  cells.forEach(cell => cell.addEventListener('click', onCellClick));

  // start polling
  if (matchId) {
    fetchState();
    pollTimer = setInterval(fetchState, pollInterval);
    window.addEventListener('beforeunload', function () { if (pollTimer) clearInterval(pollTimer); });
  }
})();
