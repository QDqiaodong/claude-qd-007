package com.nursing.home.dto;

import java.util.List;

/** 外出护送单上勾选外带药品的请求。 */
public class TakeoutMedicineRequest {

    public Long medicineId;
    public Integer qty;
    public String doseTime;
    public String operator;
    public List<TakeoutItem> items;

    public static class TakeoutItem {
        public Long medicineId;
        public Integer qty;
        public String doseTime;
        public String operator;
    }
}
