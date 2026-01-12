(function () {
  'use strict';

  const matchId = document.getElementById('matchId')?.value;
  const username = document.getElementById('username')?.value;
  const csrfToken = document.querySelector('input[name="_csrf"]')?.value;

  if (!matchId) {
    console.warn('darour: no matchId');
    return;
  }

  // i18n messages from server
  const i18n = {
    cardsSuffix: document.getElementById('i18n-cards-suffix')?.value || 'cards',
    newRound: document.getElementById('i18n-newround')?.value || 'New Round',
    turnYours: document.getElementById('i18n-turn-yours')?.value || "It's your turn",
    turnOther: document.getElementById('i18n-turn-other')?.value || "{0}'s turn",
    resultWin: document.getElementById('i18n-result-win')?.value || 'Victory!',
    resultCongrats: document.getElementById('i18n-result-congrats')?.value || 'Congratulations!',
    resultLose: document.getElementById('i18n-result-lose')?.value || 'Defeat...',
    resultWinner: document.getElementById('i18n-result-winner')?.value || '{0} wins',
    errorFetchState: document.getElementById('i18n-error-fetchstate')?.value || 'Failed to get game state',
    errorInvalidCombination: document.getElementById('i18n-error-invalidcombination')?.value || 'Invalid combination',
    successPlayed: document.getElementById('i18n-success-played')?.value || 'Cards played',
    errorPlayFailed: document.getElementById('i18n-error-playfailed')?.value || 'Failed to play cards. Please select a stronger combination.',
    errorNetwork: document.getElementById('i18n-error-network')?.value || 'A network error occurred',
    successPassed: document.getElementById('i18n-success-passed')?.value || 'Passed',
    errorPassFailed: document.getElementById('i18n-error-passfailed')?.value || 'Failed to pass',
    handSingle: document.getElementById('i18n-hand-single')?.value || 'Single',
    handPair: document.getElementById('i18n-hand-pair')?.value || 'Pair',
    handThreeOfAKind: document.getElementById('i18n-hand-threeofakind')?.value || 'Three of a Kind',
    handStraight: document.getElementById('i18n-hand-straight')?.value || 'Straight',
    handFlush: document.getElementById('i18n-hand-flush')?.value || 'Flush',
    handFullHouse: document.getElementById('i18n-hand-fullhouse')?.value || 'Full House',
    handFourOfAKind: document.getElementById('i18n-hand-fourofakind')?.value || 'Four of a Kind',
    handStraightFlush: document.getElementById('i18n-hand-straightflush')?.value || 'Straight Flush'
  };

  const POLL_INTERVAL = 1500;
  let pollTimer = null;
  let selectedCards = [];
  let gameState = null;

  const elements = {
    turnIndicator: document.getElementById('turn-indicator'),
    turnPlayerName: document.getElementById('turn-player-name'),
    opponentLeft: document.getElementById('opponent-left'),
    opponentRight: document.getElementById('opponent-right'),
    tableCards: document.getElementById('table-cards'),
    combinationType: document.getElementById('combination-type'),
    myHand: document.getElementById('my-hand'),
    myCardCount: document.getElementById('my-card-count'),
    playBtn: document.getElementById('play-btn'),
    passBtn: document.getElementById('pass-btn'),
    messageArea: document.getElementById('message-area'),
    resultOverlay: document.getElementById('result-overlay'),
    resultTitle: document.getElementById('result-title'),
    resultMessage: document.getElementById('result-message')
  };

  const COMBINATION_NAMES = {
    'HIGH_CARD': i18n.handSingle,
    'PAIR': i18n.handPair,
    'THREE_OF_A_KIND': i18n.handThreeOfAKind,
    'STRAIGHT': i18n.handStraight,
    'FLUSH': i18n.handFlush,
    'FULL_HOUSE': i18n.handFullHouse,
    'FOUR_OF_A_KIND': i18n.handFourOfAKind,
    'STRAIGHT_FLUSH': i18n.handStraightFlush
  };

  function getSuitColor(suit) {
    return (suit === '♥' || suit === '♦') ? 'red' : 'black';
  }

  function createCardElement(card, isSelectable = false) {
    const cardEl = document.createElement('div');
    cardEl.className = 'card-item';
    cardEl.dataset.serialized = card.serialized;
    cardEl.dataset.suit = card.suit;
    cardEl.dataset.rank = card.rank;

    const suitColor = getSuitColor(card.suit);
    cardEl.classList.add(suitColor);

    cardEl.innerHTML = `
      <span class="card-suit">${card.suit}</span>
      <span class="card-rank">${card.rank}</span>
    `;

    if (isSelectable) {
      cardEl.addEventListener('click', () => toggleCardSelection(cardEl, card));
    }

    return cardEl;
  }

  function toggleCardSelection(cardEl, card) {
    if (!gameState || !gameState.isMyTurn) return;

    const idx = selectedCards.findIndex(c => c.serialized === card.serialized);
    if (idx >= 0) {
      selectedCards.splice(idx, 1);
      cardEl.classList.remove('selected');
    } else {
      selectedCards.push(card);
      cardEl.classList.add('selected');
    }

    updatePlayButton();
  }

  function evaluateCombination(cards) {
    const count = cards.length;
    if (count === 0) return null;

    const ranks = cards.map(c => c.rank);
    const suits = cards.map(c => c.suit);

    const rankStrengths = {
      '3': 1, '4': 2, '5': 3, '6': 4, '7': 5, '8': 6, '9': 7,
      'T': 8, 'J': 9, 'Q': 10, 'K': 11, 'A': 12, '2': 13
    };

    const sortedByRank = [...cards].sort((a, b) =>
      rankStrengths[a.rank] - rankStrengths[b.rank]
    );

    if (count === 1) {
      return 'HIGH_CARD';
    }

    if (count === 2) {
      if (ranks[0] === ranks[1]) return 'PAIR';
      return null;
    }

    if (count === 3) {
      if (ranks[0] === ranks[1] && ranks[1] === ranks[2]) return 'THREE_OF_A_KIND';
      return null;
    }

    if (count === 5) {
      const rankCounts = {};
      ranks.forEach(r => { rankCounts[r] = (rankCounts[r] || 0) + 1; });
      const counts = Object.values(rankCounts).sort((a, b) => b - a);

      const isFlush = suits.every(s => s === suits[0]);

      const sortedStrengths = sortedByRank.map(c => rankStrengths[c.rank]);
      let isStraight = true;
      for (let i = 0; i < 4; i++) {
        if (sortedStrengths[i + 1] - sortedStrengths[i] !== 1) {
          isStraight = false;
          break;
        }
      }

      if (isStraight && isFlush) return 'STRAIGHT_FLUSH';
      if (counts[0] === 4) return 'FOUR_OF_A_KIND';
      if (counts[0] === 3 && counts[1] === 2) return 'FULL_HOUSE';
      if (isFlush) return 'FLUSH';
      if (isStraight) return 'STRAIGHT';

      return null;
    }

    return null;
  }

  function updatePlayButton() {
    if (!gameState || !gameState.isMyTurn) {
      elements.playBtn.disabled = true;
      return;
    }

    const combinationType = evaluateCombination(selectedCards);

    if (!combinationType) {
      elements.playBtn.disabled = true;
      return;
    }

    if (gameState.tableCombination) {
      const tableCardCount = gameState.tableCombination.cards.length;
      if (selectedCards.length !== tableCardCount) {
        elements.playBtn.disabled = true;
        return;
      }
    }

    elements.playBtn.disabled = false;
  }

  function renderOpponents(state) {
    const players = [state.player1, state.player2, state.player3];
    const handCounts = [state.player1HandCount, state.player2HandCount, state.player3HandCount];
    const myIdx = state.myPlayerIndex;

    const leftIdx = (myIdx + 1) % 3;
    const rightIdx = (myIdx + 2) % 3;

    const leftName = elements.opponentLeft.querySelector('.opponent-name');
    const leftCount = elements.opponentLeft.querySelector('.card-count');
    leftName.textContent = players[leftIdx];
    leftCount.textContent = handCounts[leftIdx] + i18n.cardsSuffix;

    if (state.currentPlayerIndex === leftIdx) {
      elements.opponentLeft.classList.add('current-turn');
    } else {
      elements.opponentLeft.classList.remove('current-turn');
    }

    const rightName = elements.opponentRight.querySelector('.opponent-name');
    const rightCount = elements.opponentRight.querySelector('.card-count');
    rightName.textContent = players[rightIdx];
    rightCount.textContent = handCounts[rightIdx] + i18n.cardsSuffix;

    if (state.currentPlayerIndex === rightIdx) {
      elements.opponentRight.classList.add('current-turn');
    } else {
      elements.opponentRight.classList.remove('current-turn');
    }
  }

  function renderTableCards(state) {
    elements.tableCards.innerHTML = '';

    if (!state.tableCombination || !state.tableCombination.cards) {
      elements.combinationType.textContent = i18n.newRound;
      return;
    }

    state.tableCombination.cards.forEach(card => {
      const cardEl = createCardElement(card, false);
      elements.tableCards.appendChild(cardEl);
    });

    const typeName = COMBINATION_NAMES[state.tableCombination.type] || state.tableCombination.type;
    elements.combinationType.textContent = typeName;
  }

  function renderMyHand(state) {
    elements.myHand.innerHTML = '';

    if (!state.myHand) return;

    state.myHand.forEach(card => {
      const isSelected = selectedCards.some(c => c.serialized === card.serialized);
      const cardEl = createCardElement(card, true);
      if (isSelected) {
        cardEl.classList.add('selected');
      }
      elements.myHand.appendChild(cardEl);
    });

    elements.myCardCount.textContent = '(' + state.myHand.length + i18n.cardsSuffix + ')';
  }

  function renderTurnStatus(state) {
    const players = [state.player1, state.player2, state.player3];
    const currentPlayer = players[state.currentPlayerIndex];

    if (state.isMyTurn) {
      elements.turnIndicator.textContent = '●';
      elements.turnIndicator.classList.add('my-turn');
      elements.turnPlayerName.textContent = i18n.turnYours;
      elements.passBtn.disabled = false;
    } else {
      elements.turnIndicator.textContent = '○';
      elements.turnIndicator.classList.remove('my-turn');
      elements.turnPlayerName.textContent = i18n.turnOther.replace('{0}', currentPlayer);
      elements.passBtn.disabled = true;
    }

    updatePlayButton();
  }

  function renderGameState(state) {
    gameState = state;

    selectedCards = selectedCards.filter(selected =>
      state.myHand.some(card => card.serialized === selected.serialized)
    );

    renderOpponents(state);
    renderTableCards(state);
    renderMyHand(state);
    renderTurnStatus(state);

    if (state.finished) {
      showResult(state);
    }
  }

  function showResult(state) {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = null;
    }

    elements.resultOverlay.classList.remove('hidden');

    if (state.winner === username) {
      elements.resultTitle.textContent = i18n.resultWin;
      elements.resultMessage.textContent = i18n.resultCongrats;
    } else {
      elements.resultTitle.textContent = i18n.resultLose;
      elements.resultMessage.textContent = i18n.resultWinner.replace('{0}', state.winner);
    }
  }

  function showMessage(msg, isError = false) {
    elements.messageArea.textContent = msg;
    elements.messageArea.className = isError ? 'error' : 'info';

    setTimeout(() => {
      elements.messageArea.textContent = '';
      elements.messageArea.className = '';
    }, 3000);
  }

  async function fetchState() {
    try {
      const resp = await fetch('/darour/' + encodeURIComponent(matchId) + '/state', {
        credentials: 'include',
        cache: 'no-store'
      });

      if (!resp.ok) {
        console.error('fetchState failed', resp.status);
        return;
      }

      const state = await resp.json();

      if (state.error) {
        console.error('fetchState error', state.error);
        showMessage(i18n.errorFetchState, true);
        return;
      }

      renderGameState(state);

    } catch (e) {
      console.error('fetchState exception', e);
    }
  }

  async function playCards() {
    if (selectedCards.length === 0) return;

    const combinationType = evaluateCombination(selectedCards);
    if (!combinationType) {
      showMessage(i18n.errorInvalidCombination, true);
      return;
    }

    const cardsSerialized = selectedCards.map(c => c.serialized).join(',');
    const serializedCombination = cardsSerialized + '|' + combinationType;

    elements.playBtn.disabled = true;

    try {
      const resp = await fetch('/darour?matchId=' + encodeURIComponent(matchId), {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        },
        body: JSON.stringify({
          playerName: username,
          serializedCombination: serializedCombination
        })
      });

      const result = await resp.json();

      if (result.success === 'true') {
        selectedCards = [];
        showMessage(i18n.successPlayed);
        await fetchState();
      } else {
        showMessage(i18n.errorPlayFailed, true);
        elements.playBtn.disabled = false;
      }

    } catch (e) {
      console.error('playCards error', e);
      showMessage(i18n.errorNetwork, true);
      elements.playBtn.disabled = false;
    }
  }

  async function passAction() {
    elements.passBtn.disabled = true;

    try {
      const resp = await fetch('/darour/' + encodeURIComponent(matchId) + '/pass', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': csrfToken
        }
      });

      const result = await resp.json();

      if (result.success === 'true') {
        selectedCards = [];
        showMessage(i18n.successPassed);
        await fetchState();
      } else {
        showMessage(i18n.errorPassFailed, true);
        elements.passBtn.disabled = false;
      }

    } catch (e) {
      console.error('passAction error', e);
      showMessage(i18n.errorNetwork, true);
      elements.passBtn.disabled = false;
    }
  }

  elements.playBtn.addEventListener('click', playCards);
  elements.passBtn.addEventListener('click', passAction);

  fetchState();
  pollTimer = setInterval(fetchState, POLL_INTERVAL);

  window.addEventListener('beforeunload', () => {
    if (pollTimer) clearInterval(pollTimer);
  });

})();