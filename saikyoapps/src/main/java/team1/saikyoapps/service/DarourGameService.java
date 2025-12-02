package team1.saikyoapps.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import team1.saikyoapps.model.Darour;
import team1.saikyoapps.model.DarourMapper;

/**
 * DarourGame の永続化と SSE 発行を管理するサービスクラス
 * - darourGamesCache: darourId -> stateJson の簡易キャッシュ
 * - emitters: darourId ごとの SseEmitter リスト
 *
 * 注意: DarourGameState の具体クラスがある場合は stateJson の代わりにシリアライズ/デシリアライズ処理を入れてください
 */
@Service
public class DarourGameService {

  private final DarourMapper darourMapper;

  // メモリキャッシュ: darourGameId -> stateJson
  private final Map<String, String> darourGamesCache = new ConcurrentHashMap<>();

  // darourGameId -> SseEmitter 一覧
  private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

  @Autowired
  public DarourGameService(DarourMapper darourMapper) {
    this.darourMapper = darourMapper;
  }

  /**
   * 新しい DarourGame を作成して DB に保存し、キャッシュにも格納する
   * 
   * @param ownerUserId      オーナーの userId
   * @param initialStateJson 初期 state を JSON 文字列で渡す
   * @return 生成された darourGameId
   */
  public String createDarourGame(String ownerUserId, String initialStateJson) {
    String darourGameId = UUID.randomUUID().toString();
    Darour d = new Darour();
    d.setDarourGameId(darourGameId);
    d.setOwnerUserId(ownerUserId);
    d.setStateJson(initialStateJson);
    darourMapper.insertDarour(d);
    darourGamesCache.put(darourGameId, initialStateJson);
    return darourGameId;
  }

  /**
   * キャッシュまたは DB から stateJson を取得する
   */
  public String getState(String darourGameId) {
    String s = darourGamesCache.get(darourGameId);
    if (s != null) {
      return s;
    }
    Darour d = darourMapper.selectDarourById(darourGameId);
    if (d != null) {
      darourGamesCache.put(darourGameId, d.getStateJson());
      return d.getStateJson();
    }
    return null;
  }

  /**
   * 状態を永続化する。トランザクションコミット後に全クライアントへ SSE を送信する。
   */
  @Transactional
  public void persistState(final String darourGameId, final String stateJson) {
    // DB 更新
    darourMapper.updateDarourState(darourGameId, stateJson);
    // キャッシュ更新
    darourGamesCache.put(darourGameId, stateJson);

    // コミット後に emitAll を実行するために同期処理を登録
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        emitAll(darourGameId, stateJson);
      }
    });
  }

  /**
   * SSE エミッタを登録して返す。呼び出し側は返却された SseEmitter をコントローラから返却する。
   */
  public SseEmitter registerEmitter(String darourGameId) {
    SseEmitter emitter = new SseEmitter(0L); // タイムアウトなし
    CopyOnWriteArrayList<SseEmitter> list = emitters.computeIfAbsent(darourGameId, k -> new CopyOnWriteArrayList<>());
    list.add(emitter);

    // 完了・タイムアウト時に登録解除
    emitter.onCompletion(() -> list.remove(emitter));
    emitter.onTimeout(() -> list.remove(emitter));
    emitter.onError((ex) -> list.remove(emitter));

    return emitter;
  }

  /**
   * 指定ゲームの全登録エミッタへ stateJson を送信する
   */
  public void emitAll(String darourGameId, String stateJson) {
    List<SseEmitter> list = emitters.get(darourGameId);
    if (list == null || list.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : list) {
      try {
        emitter.send(SseEmitter.event().name("darour-state").data(stateJson));
      } catch (Exception e) {
        // 送信失敗時は該当エミッタを削除して完了させる
        try {
          emitter.completeWithError(e);
        } catch (Exception ignore) {
        }
        list.remove(emitter);
      }
    }
  }

  /**
   * DarourGame を DB とキャッシュから削除する
   */
  public void removeDarourGame(String darourGameId) {
    darourMapper.deleteDarour(darourGameId);
    darourGamesCache.remove(darourGameId);
    emitters.remove(darourGameId);
  }
}
