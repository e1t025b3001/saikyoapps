(function () {
  let matchId = new URLSearchParams(window.location.search).get('matchId');
  if (!matchId) {
    const midEl = document.getElementById('matchId');
    if (midEl && midEl.value) matchId = midEl.value;
  }

  const cells = Array.from(document.querySelectorAll('.cell'));
  const statusElem = document.getElementById('currentPlayer');
  const resultElem = document.getElementById('result');

  let pollInterval = 1500;
  let pollTimer = null;

  async function fetchState() {
    if (!matchId) return;
    try {
      const res = await fetch('/marubatsu/' + encodeURIComponent(matchId), { cache: 'no-store' });
      if (!res.ok) { console.error('fetchState failed', res.status, await res.text()); return; }
      const j = await res.json();
      if (!j) return;
      const board = j.board || new Array(9).fill(0);
      cells.forEach(cell => {
        const r = parseInt(cell.getAttribute('data-row'));
        const c = parseInt(cell.getAttribute('data-col'));
        const idx = r * 3 + c;
        const v = board[idx] || 0;
        cell.textContent = v === 1 ? 'X' : v === 2 ? 'O' : '';
        if (v !== 0 || j.finished) cell.classList.add('disabled'); else cell.classList.remove('disabled');
      });

      if (j.finished) {
        if (j.winner) resultElem.textContent = j.winner + ' の勝ち！'; else resultElem.textContent = '引き分けです。';
        if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
        return;
      }

      const turn = j.turn;
      if (turn === 'X') statusElem.textContent = '先手 (X): ' + (j.playerX || '');
      else statusElem.textContent = '後手 (O): ' + (j.playerO || '');

    } catch (e) { console.error(e); }
  }

  async function sendMove(pos) {
    if (!matchId) { alert('matchId がありません'); return; }
    try {
      const tokenEl = document.querySelector('input[name="_csrf"]');
      const token = tokenEl ? tokenEl.value : null;
      const resp = await fetch('/marubatsu/' + encodeURIComponent(matchId) + '/move', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
        body: JSON.stringify({ pos: pos })
      });
      const j = await resp.json();
      if (j.error) { alert('Error: ' + j.error); fetchState(); }
    } catch (e) { console.error(e); alert('通信エラーが発生しました'); }
  }

  function onCellClick(e) {
    const cell = e.currentTarget;
    if (cell.classList.contains('disabled')) return;
    const r = parseInt(cell.getAttribute('data-row'));
    const c = parseInt(cell.getAttribute('data-col'));
    const idx = r * 3 + c;
    cell.classList.add('disabled');
    sendMove(idx);
  }

  cells.forEach(cell => cell.addEventListener('click', onCellClick));

  if (matchId) { fetchState(); pollTimer = setInterval(fetchState, pollInterval); window.addEventListener('beforeunload', function () { if (pollTimer) clearInterval(pollTimer); }); }
  else console.warn('marubatsu: no matchId detected');
})();
