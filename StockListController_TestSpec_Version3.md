# 検査仕様書: StockListController

対象ファイル:
- DroneInventorySystem/src/main/java/com/digitalojt/web/controller/StockListController.java

概要:
- 在庫一覧画面 (index)、検索 (search)、在庫登録 (register: GET/POST)、在庫更新 (update: GET/PATCH) を提供するコントローラ。
- 主な連携先サービス:
  - StockListService (service)
  - PartsCategoryService (partsCategoryService)
  - CenterInfoService (centerInfoService)
- 主に使用するモデル属性:
  - ModelAttributeContents.STOCK_ITEM_LIST（在庫リスト）
  - ModelAttributeContents.STOCK_ITEM_UPDATE_FORM（登録/更新用フォーム）
  - "categories"（カテゴリ一覧）
  - "centers"（拠点一覧）
  - ModelAttributeContents.ERROR_MSG / ModelAttributeContents.SUCCESS_MSG（フラッシュ/表示用メッセージ）
- 画面リターンは UrlConsts の各定数（STOCK_LIST, STOCK_LIST_INDEX, STOCK_LIST_REGISTER, STOCK_LIST_UPDATE, STOCK_LIST_SEARCH）を用いる。

前提:
- Spring MVC (Controller)、Jakarta Validation (@Valid)、BindingResult を使用。
- テストは @WebMvcTest / MockMvc と Mockito で行う（外部依存は MockBean）。
- 実行時に UrlConsts / ModelAttributeContents の文字列はプロジェクト定義に依存するため、テスト内で定数を直接参照するか、実際の文字列に置換する。

抽出されたエンドポイント（コントローラ内定義）:
- GET  UrlConsts.STOCK_LIST
  - メソッド: index(Model)
  - 動作:
    - service.getAllStockListData() を呼び出し、ModelAttributeContents.STOCK_ITEM_LIST に追加
    - partsCategoryService.getCategoryInfoData() を "categories" に追加
    - ビュー: UrlConsts.STOCK_LIST_INDEX

- GET  UrlConsts.STOCK_LIST_SEARCH
  - メソッド: search(@Valid StockListForm form, BindingResult bindingResult, Model model)
  - パラメータ:
    - StockListForm のフィールド (少なくとも categoryId, partName, quantity, quantityOp を使用)
    - Validation:@Valid を利用。bindingResult.hasErrors() が true ならエラーメッセージを ModelAttributeContents.ERROR_MSG に設定して UrlConsts.STOCK_LIST_INDEX を返す
  - 動作:
    - categoryId, quantity を Integer に変換（不正数は null とする）
    - service.searchStockList(catId, form.getPartName(), form.getQuantityOp(), qty) を呼ぶ
    - 結果を ModelAttributeContents.STOCK_ITEM_LIST と "stockList" に追加
    - partsCategoryService.getCategoryInfoData() を "categories" に追加
    - ビュー: UrlConsts.STOCK_LIST_INDEX

- GET  UrlConsts.STOCK_LIST_REGISTER
  - メソッド: register(Model)
  - 動作:
    - 空の StockListForm を作成し ModelAttributeContents.STOCK_ITEM_UPDATE_FORM に追加
    - partsCategoryService.getCategoryInfoData() を "categories" に追加
    - centerInfoService.getCenterInfoData() を "centers" に追加
    - ビュー: UrlConsts.STOCK_LIST_REGISTER

- POST UrlConsts.STOCK_LIST_REGISTER
  - メソッド: register(Model, @Valid StockListForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes)
  - 動作:
    - バリデーションエラー時:
      - redirectAttributes に ModelAttributeContents.ERROR_MSG を addFlashAttribute
      - "redirect:" + UrlConsts.STOCK_LIST_REGISTER を返す
    - 正常時:
      - service.stockDuplicationCheck(form.getPartName(), null)
      - service.registerStockItem(form)
      - redirectAttributes.addFlashAttribute(ModelAttributeContents.SUCCESS_MSG, "success.register")
      - "redirect:" + UrlConsts.STOCK_LIST を返す

- GET  UrlConsts.STOCK_LIST_UPDATE + "/{stockId}"
  - メソッド: update(@PathVariable int stockId, Model model)
  - 動作:
    - service.getStockItemData(stockId) を呼び、戻り値 (StockItem) で StockListForm を作成して初期化
    - ModelAttributeContents.STOCK_ITEM_UPDATE_FORM に追加
    - partsCategoryService.getCategoryInfoData() を "categories" に追加
    - centerInfoService.getCenterInfoData() を "centers" に追加
    - ビュー: UrlConsts.STOCK_LIST_UPDATE

- PATCH UrlConsts.STOCK_LIST_UPDATE
  - メソッド: update(Model, @Valid StockListForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes)
  - 動作:
    - バリデーションエラー時:
      - redirectAttributes に ModelAttributeContents.ERROR_MSG を addFlashAttribute
      - "redirect:" + UrlConsts.STOCK_LIST_UPDATE + "/" + form.getStockId() を返す
    - 正常時:
      - service.updateStockItem(form)
      - redirectAttributes.addFlashAttribute(ModelAttributeContents.SUCCESS_MSG, "success.update")
      - "redirect:" + UrlConsts.STOCK_LIST を返す

内部ユーティリティ:
- private String getValidationErrorMessage(BindingResult bindingResult, @Valid StockListForm form)
  - bindingResult の fieldErrors を結合して \r\n 区切りのメッセージ文字列を返す
  - ログ出力あり

検査対象観点（抜粋）:
- 正常系: 各 GET が View と Model を正しく返すこと
- 検索: 数値変換（不正文字 -> null）と service.searchStockList の呼出し引数の整合性
- 登録:
  - GET: 空フォーム・カテゴリ・拠点が Model に設定される
  - POST: バリデーション時のフラッシュメッセージとリダイレクト
  - POST: 重複チェックが呼ばれること、登録が呼ばれること、成功時フラッシュと一覧へリダイレクト
- 更新:
  - GET: service.getStockItemData の結果がフォームへ正しくマッピングされる
  - PATCH: バリデーションエラー時のリダイレクト先、成功時の service.updateStockItem 呼出とリダイレクト
- 例外系: Service が例外を投げた場合に 5xx が返ること（Controller レベルで捕捉していないため）
- ログ: 各メソッドで logStart/logEnd が呼ばれていること（必要に応じてログの検証を行う）

テストケース（ID・手順・期待結果）
- TC-SLC-001: index 正常表示
  - 前提: service.getAllStockListData が在庫リストを返す
  - 手順: GET UrlConsts.STOCK_LIST
  - 期待: HTTP 200、ビュー UrlConsts.STOCK_LIST_INDEX、ModelAttributeContents.STOCK_ITEM_LIST にリスト、"categories" にカテゴリ一覧

- TC-SLC-002: search バリデーションエラー
  - 前提: bindingResult がエラーを返す（@WebMvcTest では不正入力を送る）
  - 手順: GET UrlConsts.STOCK_LIST_SEARCH with invalid form params
  - 期待: ビュー UrlConsts.STOCK_LIST_INDEX、ModelAttributeContents.ERROR_MSG がセット

- TC-SLC-003: search 数値変換エラーは null として service に渡される
  - 前提: form.categoryId = "abc", form.quantity = "12x"
  - 手順: GET UrlConsts.STOCK_LIST_SEARCH with those params
  - 期待: service.searchStockList が catId=null, qty=null で呼ばれる、戻り値が Model に設定される

- TC-SLC-004: register GET フォーム表示
  - 手順: GET UrlConsts.STOCK_LIST_REGISTER
  - 期待: ModelAttributeContents.STOCK_ITEM_UPDATE_FORM（空フォーム）、"categories","centers" が Model に存在

- TC-SLC-005: register POST バリデーションエラー リダイレクトとフラッシュ
  - 前提: 不正な入力（BindingResult エラー）
  - 手順: POST UrlConsts.STOCK_LIST_REGISTER with invalid form
  - 期待: redirect:UrlConsts.STOCK_LIST_REGISTER、flash に ModelAttributeContents.ERROR_MSG

- TC-SLC-006: register POST 正常
  - 前提: 正常入力
  - 手順: POST UrlConsts.STOCK_LIST_REGISTER with valid form
  - 期待:
    - service.stockDuplicationCheck(form.getPartName(), null) が呼ばれる
    - service.registerStockItem(form) が呼ばれる
    - redirect: UrlConsts.STOCK_LIST、flash に ModelAttributeContents.SUCCESS_MSG ("success.register")

- TC-SLC-007: update GET フォームに値が入る
  - 前提: service.getStockItemData(stockId) が StockItem を返す
  - 手順: GET UrlConsts.STOCK_LIST_UPDATE + "/{stockId}"
  - 期待:
    - ModelAttributeContents.STOCK_ITEM_UPDATE_FORM が StockItem の値で初期化される
    - "categories","centers" が Model に存在

- TC-SLC-008: update PATCH バリデーションエラーは元の更新画面へリダイレクト
  - 前提: BindingResult エラー
  - 手順: PATCH UrlConsts.STOCK_LIST_UPDATE with invalid form (stockId present)
  - 期待: redirect: UrlConsts.STOCK_LIST_UPDATE + "/" + form.getStockId(), flash に ModelAttributeContents.ERROR_MSG

- TC-SLC-009: update PATCH 正常
  - 前提: 正常入力
  - 手順: PATCH UrlConsts.STOCK_LIST_UPDATE with valid form
  - 期待:
    - service.updateStockItem(form) が呼ばれる
    - redirect: UrlConsts.STOCK_LIST、flash に ModelAttributeContents.SUCCESS_MSG ("success.update")

- TC-SLC-010: Service が例外を投げた場合の 5xx 応答
  - 前提: service.getAllStockListData() が RuntimeException を投げる
  - 手順: GET UrlConsts.STOCK_LIST
  - 期待: HTTP 5xx

実行上の注意・工数見積り:
- 単体テスト (MockMvc) の作成は 1〜2 時間（10〜15 テストケース）
- Integration テスト (SpringBootTest + H2) の追加は 2〜4 時間（DB マッピング・初期データ準備含む）
- セキュリティ適用がある場合は追加作業（認証コンテキストのセットアップ）

次のアクション:
- 付属のテストスケルトン（Java）をプロジェクトへ追加し、UrlConsts の文字列がテスト実行環境で解決されることを確認してください。必要ならテスト内で文字列を直接使うよう置換します。
- テストを実行して失敗する箇所を確認・修正し、期待値を調整してください。
