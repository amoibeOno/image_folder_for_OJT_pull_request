# 検査仕様書（日本語）: StockListController

対象ファイル:
- DroneInventorySystem/src/main/java/com/digitalojt/web/controller/StockListController.java

目的:
- StockListController の各エンドポイント（一覧表示、検索、登録、更新）について、正常系/異常系を網羅した検査ケースを定義し、実装テスト（単体/結合）へ落とし込めるようにする。

前提・環境:
- フレームワーク: Spring MVC（@Controller）、Jakarta Validation（@Valid）、BindingResult を使用
- テスト: JUnit5、Mockito、Spring Test (MockMvc) を推奨
- サービスは Mock（@MockBean）で置き換え、Controller 単体の挙動を検証する
- 必要に応じて Integration テストは @SpringBootTest + H2（または Testcontainers）で実行
- 定数: UrlConsts, ModelAttributeContents はプロジェクト定義に依存。テスト内で参照するか文字列で直接指定する

抽出されたエンドポイント一覧（コントローラ実装に基づく）:
1. GET  UrlConsts.STOCK_LIST
   - メソッド: index(Model)
   - 動作:
     - service.getAllStockListData() を呼ぶ
     - ModelAttributeContents.STOCK_ITEM_LIST に在庫一覧をセット
     - "categories" に partsCategoryService.getCategoryInfoData() をセット
     - 返却ビュー: UrlConsts.STOCK_LIST_INDEX

2. GET  UrlConsts.STOCK_LIST_SEARCH
   - メソッド: search(@Valid StockListForm form, BindingResult bindingResult, Model model)
   - 入力:
     - StockListForm のフィールド（少なくとも categoryId, partName, quantity, quantityOp）
   - 動作:
     - bindingResult にエラーがあれば ModelAttributeContents.ERROR_MSG にメッセージをセットし UrlConsts.STOCK_LIST_INDEX を返す
     - categoryId, quantity は Integer に変換（NumberFormatException -> null）
     - service.searchStockList(catId, partName, quantityOp, qty) を呼ぶ
     - 結果を ModelAttributeContents.STOCK_ITEM_LIST と "stockList" にセット
     - "categories" に partsCategoryService.getCategoryInfoData() をセット
     - 返却ビュー: UrlConsts.STOCK_LIST_INDEX

3. GET  UrlConsts.STOCK_LIST_REGISTER
   - メソッド: register(Model)
   - 動作:
     - 空の StockListForm を作成し ModelAttributeContents.STOCK_ITEM_UPDATE_FORM にセット
     - "categories": partsCategoryService.getCategoryInfoData()
     - "centers": centerInfoService.getCenterInfoData()
     - 返却ビュー: UrlConsts.STOCK_LIST_REGISTER

4. POST UrlConsts.STOCK_LIST_REGISTER
   - メソッド: register(Model, @Valid StockListForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes)
   - 動作:
     - バリデーション error -> redirectAttributes に ModelAttributeContents.ERROR_MSG を addFlashAttribute、"redirect:"+UrlConsts.STOCK_LIST_REGISTER を返す
     - 正常 -> service.stockDuplicationCheck(form.getPartName(), null) を実行（重複チェック）
     - service.registerStockItem(form) を実行（登録）
     - redirectAttributes に ModelAttributeContents.SUCCESS_MSG("success.register") をセット
     - "redirect:"+UrlConsts.STOCK_LIST を返す

5. GET  UrlConsts.STOCK_LIST_UPDATE + "/{stockId}"
   - メソッド: update(@PathVariable int stockId, Model model)
   - 動作:
     - service.getStockItemData(stockId) を呼び、戻り値 StockItem を元に StockListForm を初期化
     - ModelAttributeContents.STOCK_ITEM_UPDATE_FORM にセット
     - "categories": partsCategoryService.getCategoryInfoData()
     - "centers": centerInfoService.getCenterInfoData()
     - 返却ビュー: UrlConsts.STOCK_LIST_UPDATE

6. PATCH UrlConsts.STOCK_LIST_UPDATE
   - メソッド: update(Model, @Valid StockListForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes)
   - 動作:
     - バリデーション error -> redirectAttributes に ModelAttributeContents.ERROR_MSG を addFlashAttribute、"redirect:"+UrlConsts.STOCK_LIST_UPDATE+"/"+form.getStockId() を返す
     - 正常 -> service.updateStockItem(form) を実行
     - redirectAttributes に ModelAttributeContents.SUCCESS_MSG("success.update") をセット
     - "redirect:"+UrlConsts.STOCK_LIST を返す

内部ユーティリティ:
- getValidationErrorMessage(BindingResult, StockListForm)
  - bindingResult の fieldErrors を結合して改行(\r\n)区切りのエラーメッセージ文字列を作成し返す

検査観点（網羅リスト）:
- 正常表示系
  - 一覧（index）が View と Model を正しく返す
  - 登録画面（GET）で空フォームと参照データ（カテゴリ・拠点）がセットされる
  - 更新画面（GET）で service のデータがフォームへマッピングされる
- 入力/検索系
  - search のバリデーション：@Valid と BindingResult のハンドリング
  - 数値変換：categoryId / quantity が数値でない場合は null として service に渡す
  - search が service.searchStockList を適切な引数で呼ぶこと
- 登録/更新系
  - register POST のバリデーションエラー時のリダイレクトとフラッシュメッセージ
  - register POST の重複チェック呼出と registerStockItem 呼出、成功時リダイレクトとフラッシュ
  - update PATCH のバリデーションエラー時リダイレクト先（/update/{id}）とフラッシュ
  - update PATCH の updateStockItem 呼出と成功時リダイレクト・フラッシュ
- 例外系
  - Service が RuntimeException 等を投げた場合に 5xx 応答が返ること（コントローラでは明示的に捕捉していないため）
- UI/Model 要件
  - ModelAttributeContents.STOCK_ITEM_LIST, STOCK_ITEM_UPDATE_FORM, ERROR_MSG, SUCCESS_MSG が正しく使用されること
  - "categories","centers","stockList" 等の属性が適切にセットされていること

テストケース（日本語で詳細）

- TC-01: 在庫一覧（index）表示（正常）
  - 種別: 単体（WebMvcTest / MockMvc）
  - 前提: stockListService.getAllStockListData() が 1 件以上の StockItem を返すようモック
  - 手順:
    1. GET リクエストを UrlConsts.STOCK_LIST に送信
  - 期待結果:
    - HTTP 200
    - ビュー名が UrlConsts.STOCK_LIST_INDEX
    - Model に ModelAttributeContents.STOCK_ITEM_LIST が存在し、返却されたリストが格納されている
    - Model に "categories" が存在（partsCategoryService の結果）
  - 優先度: 高

- TC-02: 検索（search）バリデーションエラー時の挙動
  - 種別: 単体
  - 前提: 無効な入力（例: 必須フィールド空、文字列長超過等）を送る
  - 手順:
    1. GET UrlConsts.STOCK_LIST_SEARCH に無効パラメータを送信
  - 期待結果:
    - ビューは UrlConsts.STOCK_LIST_INDEX を返す
    - ModelAttributeContents.ERROR_MSG が Model にセットされる（エラーメッセージ）
  - 優先度: 高

- TC-03: 検索で categoryId/quantity に不正文字列を与えたときは null として service に渡る
  - 種別: 単体
  - 手順:
    1. GET UrlConsts.STOCK_LIST_SEARCH?categoryId=abc&partName=xxx&quantity=12x&quantityOp=>
  - 期待結果:
    - service.searchStockList が catId=null、qty=null、partName="xxx"、quantityOp=">" で呼ばれる
    - ビューは UrlConsts.STOCK_LIST_INDEX、ModelAttributeContents.STOCK_ITEM_LIST に結果が入る
  - 優先度: 高

- TC-04: 登録画面（GET）表示
  - 種別: 単体
  - 手順:
    1. GET UrlConsts.STOCK_LIST_REGISTER
  - 期待結果:
    - HTTP 200
    - ビュー UrlConsts.STOCK_LIST_REGISTER
    - Model に ModelAttributeContents.STOCK_ITEM_UPDATE_FORM（空の StockListForm）がセットされる
    - Model に "categories" と "centers" がセットされる
  - 優先度: 中

- TC-05: 登録（POST） バリデーションエラー
  - 種別: 単体
  - 手順:
    1. POST UrlConsts.STOCK_LIST_REGISTER に不正なフォームを送信（例: partName が空）
  - 期待結果:
    - リダイレクト "redirect:" + UrlConsts.STOCK_LIST_REGISTER が返る
    - redirectAttributes（flash）に ModelAttributeContents.ERROR_MSG が含まれる
  - 優先度: 高

- TC-06: 登録（POST） 正常処理
  - 種別: 単体
  - 前提: 正常なフォーム入力
  - 手順:
    1. POST UrlConsts.STOCK_LIST_REGISTER に正常なフォームを送信
  - 期待結果:
    - service.stockDuplicationCheck(form.getPartName(), null) が呼ばれる
    - service.registerStockItem(form) が呼ばれる
    - リダイレクト "redirect:" + UrlConsts.STOCK_LIST が返る
    - flash に ModelAttributeContents.SUCCESS_MSG ("success.register") が含まれる
  - 優先度: 高

- TC-07: 更新画面（GET）でフォームがサービスデータで初期化される
  - 種別: 単体
  - 前提: stockListService.getStockItemData(stockId) が StockItem を返すようモック
  - 手順:
    1. GET UrlConsts.STOCK_LIST_UPDATE + "/{stockId}" を呼ぶ
  - 期待結果:
    - ビュー UrlConsts.STOCK_LIST_UPDATE
    - ModelAttributeContents.STOCK_ITEM_UPDATE_FORM に StockItem の値が正しく設定される（stockId, categoryId, partName, centerId, description, quantity, deleteFlag）
    - "categories","centers" が Model にある
  - 優先度: 中

- TC-08: 更新（PATCH） バリデーションエラー
  - 種別: 単体
  - 手順:
    1. PATCH UrlConsts.STOCK_LIST_UPDATE に不正なフォーム（e.g., partName 空）を送信
  - 期待結果:
    - リダイレクト "redirect:" + UrlConsts.STOCK_LIST_UPDATE + "/" + form.getStockId()
    - flash に ModelAttributeContents.ERROR_MSG がある
  - 優先度: 高

- TC-09: 更新（PATCH） 正常処理
  - 種別: 単体
  - 手順:
    1. PATCH UrlConsts.STOCK_LIST_UPDATE に正常なフォームを送信
  - 期待結果:
    - service.updateStockItem(form) が呼ばれる
    - リダイレクト "redirect:" + UrlConsts.STOCK_LIST が返る
    - flash に ModelAttributeContents.SUCCESS_MSG ("success.update") がある
  - 優先度: 高

- TC-10: Service 層例外時の挙動
  - 種別: 単体
  - 前提: 例えば stockListService.getAllStockListData() が RuntimeException を投げるようモック
  - 手順:
    1. GET UrlConsts.STOCK_LIST
  - 期待結果:
    - HTTP 5xx（コントローラ内で捕捉していないため、サーバエラーが返る）
  - 優先度: 中

追加の検査（任意）:
- ログ出力確認: 各メソッドで logStart/logEnd の呼出しが行われているか（必要ならログ出力の検証）
- 画面側表示文言の国際化（メッセージキー success.register など）の確認
- 境界値テスト: quantity に極端な値（0, 最大値, 負値）を送る場合の挙動
- 同時更新・排他（同一在庫を複数更新するケース）に対する振る舞い（要ビジネス要件確認）

テスト実装メモ:
- 単体テスト: @WebMvcTest(StockListController.class) + @MockBean(Service) + MockMvc を利用
  - ModelAttributeContents と UrlConsts はプロジェクトの定義をインポートして利用する
  - フォーム送信は contentType = MediaType.APPLICATION_FORM_URLENCODED を指定して .param(...) で行う
- Integration: @SpringBootTest(webEnvironment = RANDOM_PORT) と TestRestTemplate または MockMvc を利用。DB には H2 を使い初期データを投入
- BindingResult のエラー検出: 実アノテーション（@NotEmpty, @Size 等）に基づく不正データを送ることで検出可能
- service の振る舞い検証は Mockito.verify(...) を利用

サンプルテストデータ例:
- StockItem{id:1, sku:"DRN-001", name:"DroneA", category:{categoryId:1, name:"Quadcopter"}, center:{centerId:1, name:"Tokyo"}, amount:50, description:""} 
- StockListForm の値例（登録/更新用）:
  - partName=DroneA, categoryId=1, centerId=1, quantity=50, description="test", deleteFlag=false

次のアクション（推奨）:
1. 上記のテストケースを元に MockMvc テストコードを作成しプロジェクトで実行してください。
2. 実行時に UrlConsts や ModelAttributeContents の定数がテスト環境で解決できない場合、テスト内で実際の URL/属性名文字列に置き換えてください。
3. テスト実行結果（失敗したケースのスタックトレースや期待・実際の差分）を共有いただければ、修正案や期待値の調整、さらなる Integration テストコードを提供します。

作成者: ChatGPT (解析元: 提供された StockListController.java)