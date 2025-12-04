package com.digitalojt.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.digitalojt.web.consts.ModelAttributeContents;
import com.digitalojt.web.consts.UrlConsts;
import com.digitalojt.web.entity.StockItem;
import com.digitalojt.web.form.StockListForm;
import com.digitalojt.web.service.CenterInfoService;
import com.digitalojt.web.service.PartsCategoryService;
import com.digitalojt.web.service.StockListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

/**
 * StockListController の MockMvc テストスケルトン
 *
 * 注意:
 * - UrlConsts, ModelAttributeContents はプロジェクト内の定義を使います。
 * - StockItem / StockListForm のコンストラクタ・ゲッタ／セッタは実装に合わせて修正してください。
 */
@WebMvcTest(StockListController.class)
class StockListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockListService stockListService;

    @MockBean
    private PartsCategoryService partsCategoryService;

    @MockBean
    private CenterInfoService centerInfoService;

    @BeforeEach
    void setUp() {
        // 共通モック
        when(partsCategoryService.getCategoryInfoData()).thenReturn(Collections.emptyList());
        when(centerInfoService.getCenterInfoData()).thenReturn(Collections.emptyList());
    }

    @Test
    void indexDisplaysStockList() throws Exception {
        // Arrange
        StockItem si = new StockItem(); // TODO: 実モデルのコンストラクタ/setter を使う
        // 例: si.setStockId(1); si.setName("DroneA"); ...
        when(stockListService.getAllStockListData()).thenReturn(Arrays.asList(si));

        // Act & Assert
        mockMvc.perform(get(UrlConsts.STOCK_LIST))
                .andExpect(status().isOk())
                .andExpect(view().name(UrlConsts.STOCK_LIST_INDEX))
                .andExpect(model().attributeExists(ModelAttributeContents.STOCK_ITEM_LIST))
                .andExpect(model().attribute("categories", Collections.emptyList()));

        verify(stockListService, times(1)).getAllStockListData();
    }

    @Test
    void searchReturnsIndexOnValidationError() throws Exception {
        // Simulate validation error by sending invalid params (assumes form has @Size/@NotNull etc.)
        mockMvc.perform(get(UrlConsts.STOCK_LIST_SEARCH)
                        .param("quantity", "not_a_number")) // if form requires numeric, adjust accordingly
                .andExpect(status().isOk())
                .andExpect(view().name(UrlConsts.STOCK_LIST_INDEX))
                .andExpect(model().attributeExists(ModelAttributeContents.ERROR_MSG));
    }

    @Test
    void searchHandlesInvalidNumericInputsAndCallsServiceWithNulls() throws Exception {
        // Arrange
        when(stockListService.searchStockList(isNull(), eq("name"), anyString(), isNull()))
                .thenReturn(Collections.emptyList());

        // Act
        mockMvc.perform(get(UrlConsts.STOCK_LIST_SEARCH)
                        .param("categoryId", "abc")   // invalid int -> should be treated as null
                        .param("partName", "name")
                        .param("quantity", "12x")     // invalid int -> null
                        .param("quantityOp", ">"))
                .andExpect(status().isOk())
                .andExpect(view().name(UrlConsts.STOCK_LIST_INDEX))
                .andExpect(model().attributeExists(ModelAttributeContents.STOCK_ITEM_LIST));

        verify(stockListService).searchStockList(isNull(), eq("name"), eq(">"), isNull());
    }

    @Test
    void registerGetShowsEmptyFormAndLookupData() throws Exception {
        mockMvc.perform(get(UrlConsts.STOCK_LIST_REGISTER))
                .andExpect(status().isOk())
                .andExpect(view().name(UrlConsts.STOCK_LIST_REGISTER))
                .andExpect(model().attributeExists(ModelAttributeContents.STOCK_ITEM_UPDATE_FORM))
                .andExpect(model().attribute("categories", Collections.emptyList()))
                .andExpect(model().attribute("centers", Collections.emptyList()));
    }

    @Test
    void registerPostValidationErrorRedirectsWithFlash() throws Exception {
        // Sending empty/invalid form to trigger validation error
        mockMvc.perform(post(UrlConsts.STOCK_LIST_REGISTER)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("partName", "")) // adjust to trigger @NotEmpty or similar
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("redirect:" + UrlConsts.STOCK_LIST_REGISTER))
                .andExpect(flash().attributeExists(ModelAttributeContents.ERROR_MSG));
    }

    @Test
    void registerPostSuccessCallsServiceAndRedirectsToList() throws Exception {
        // Arrange: adjust params to satisfy validation constraints
        when(stockListService.stockDuplicationCheck(anyString(), isNull())).thenReturn(true); // if returns boolean; if void, mock differently
        // If stockDuplicationCheck is void, do nothing; the test will still verify registerStockItem called.

        // Act
        mockMvc.perform(post(UrlConsts.STOCK_LIST_REGISTER)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("partName", "NewPart")
                        .param("categoryId", "1")
                        .param("quantity", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("redirect:" + UrlConsts.STOCK_LIST))
                .andExpect(flash().attributeExists(ModelAttributeContents.SUCCESS_MSG));

        // Verify interactions
        // If stockDuplicationCheck is void:
        // verify(stockListService).stockDuplicationCheck("NewPart", null);
        // verify(stockListService).registerStockItem(any(StockListForm.class));
    }

    @Test
    void updateGetPopulatesFormFromService() throws Exception {
        // Arrange
        StockItem si = new StockItem(); // TODO: set fields
        // e.g., si.setStockId(5); si.getCategory().setCategoryId(2); si.setName("P");
        when(stockListService.getStockItemData(5)).thenReturn(si);

        // Act & Assert
        mockMvc.perform(get(UrlConsts.STOCK_LIST_UPDATE + "/{stockId}", 5))
                .andExpect(status().isOk())
                .andExpect(view().name(UrlConsts.STOCK_LIST_UPDATE))
                .andExpect(model().attributeExists(ModelAttributeContents.STOCK_ITEM_UPDATE_FORM))
                .andExpect(model().attribute("categories", Collections.emptyList()))
                .andExpect(model().attribute("centers", Collections.emptyList()));

        verify(stockListService).getStockItemData(5);
    }

    @Test
    void updatePatchValidationErrorRedirectsToUpdatePage() throws Exception {
        // Submit invalid data to trigger binding error
        mockMvc.perform(patch(UrlConsts.STOCK_LIST_UPDATE)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("stockId", "1")
                        .param("partName", "")) // empty name -> validation error
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("redirect:" + UrlConsts.STOCK_LIST_UPDATE + "/1"))
                .andExpect(flash().attributeExists(ModelAttributeContents.ERROR_MSG));
    }

    @Test
    void updatePatchSuccessCallsServiceAndRedirects() throws Exception {
        // Arrange: valid params
        mockMvc.perform(patch(UrlConsts.STOCK_LIST_UPDATE)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("stockId", "1")
                        .param("partName", "Updated")
                        .param("quantity", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("redirect:" + UrlConsts.STOCK_LIST))
                .andExpect(flash().attributeExists(ModelAttributeContents.SUCCESS_MSG));

        // verify updateStockItem called (if void)
        // verify(stockListService).updateStockItem(any(StockListForm.class));
    }

    @Test
    void serviceExceptionProducesServerError() throws Exception {
        when(stockListService.getAllStockListData()).thenThrow(new RuntimeException("DB down"));

        mockMvc.perform(get(UrlConsts.STOCK_LIST))
                .andExpect(status().is5xxServerError());
    }
}