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

  // 安全的 fetch，會回傳 {ok, status, text, json}
  async function fetchSafe(url, opts) {
    const res = await fetch(url, Object.assign({ credentials: 'include' }, opts || {}));
    const status = res.status;
    let text = null;
    let json = null;
    try { text = await res.text(); } catch (e) { /* ignore */ }
    try { if (res.headers.get('content-type') && res.headers.get('content-type').includes('application/json')) json = JSON.parse(text); } catch (e) { json = null; }
    return { ok: res.ok, status, text, json };
  }

  // 當 fetch /marubatsu/{id} 回 404 或錯誤時的 fallback 處理
  async function finishFallback() {
    // 停止輪詢
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    resultElem.textContent = '對局狀態異常，正在確認...';
    try {
      const r = await fetchSafe('/matching/status?game=marubatsu', { cache: 'no-store' });
      if (r && r.json) {
        const s = r.json.status;
        if (s === 'playing' && r.json.gameId) {
          // 重新設定 matchId 並繼續輪詢
          matchId = r.json.gameId;
          resultElem.textContent = '已取得對局，重新連線中...';
          fetchState();
          pollTimer = setInterval(fetchState, pollInterval);
          return;
        }
        if (s === 'lobby') {
          resultElem.textContent = '對局已結束，5 秒後返回大廳';
          setTimeout(() => { window.location.href = '/'; }, 5000);
          return;
        }
      }
    } catch (e) {
      console.warn('finishFallback failed', e);
    }
    resultElem.textContent = '對局已結束或找不到對局，5 秒後返回大廳';
    setTimeout(() => { window.location.href = '/'; }, 5000);
  }

  async function fetchState() {
    if (!matchId) return;
    try {
      const res = await fetchSafe('/marubatsu/' + encodeURIComponent(matchId), { cache: 'no-store' });
      if (!res.ok) {
        console.error('fetchState failed', res.status, res.text);
        await finishFallback();
        return;
      }
      const j = res.json;
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
        // 自動返回大廳
        setTimeout(() => { window.location.href = '/'; }, 5000);
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
      const resp = await fetchSafe('/marubatsu/' + encodeURIComponent(matchId) + '/move', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
        body: JSON.stringify({ pos: pos })
      });
      if (!resp.ok) { console.error('move failed', resp.status, resp.text); await finishFallback(); return; }
      const j = resp.json || {};
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
