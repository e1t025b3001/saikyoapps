// marubatsu.js
(function () {
  let matchId = new URLSearchParams(window.location.search).get('matchId');
  // URL にない場合は hidden input から取得する
  if (!matchId) {
    const midEl = document.getElementById('matchId');
    if (midEl && midEl.value) matchId = midEl.value;
  }

  const boardElem = document.getElementById('board');
  const cells = Array.from(document.querySelectorAll('.cell'));
  const statusElem = document.getElementById('currentPlayer');
  const resultElem = document.getElementById('result');

  let pollInterval = 1500;
  let pollTimer = null;

  async function fetchState() {
    if (!matchId) return;
    try {
      const res = await fetch('/api/match/state?matchId=' + encodeURIComponent(matchId), { cache: 'no-store' });
      if (!res.ok) {
        console.error('fetchState failed', res.status, await res.text());
        return;
      }
      const j = await res.json();
      console.log('fetchState', j);
      if (!j) return;
      const board = j.board || [];
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
    if (!matchId) { alert('matchId がありません'); return; }
    try {
      const tokenEl = document.querySelector('input[name="_csrf"]');
      const token = tokenEl ? tokenEl.value : null;
      console.log('sendMove', { matchId, pos, token });
      const resp = await fetch('/api/match/move', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': token
        },
        body: JSON.stringify({ matchId: matchId, pos: pos })
      });
      const j = await resp.json();
      console.log('move response', j);
      if (j.error) {
        // エラーメッセージを画面に表示
        alert('Error: ' + j.error);
        // 失敗したら直ちに再描画
        fetchState();
      }
      // state は次の poll で取得される
    } catch (e) {
      console.error(e);
      alert('通信エラーが発生しました');
    }
  }

  function onCellClick(e) {
    const cell = e.currentTarget;
    if (cell.classList.contains('disabled')) return;
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
  } else {
    console.warn('marubatsu: no matchId detected');
  }
})();
