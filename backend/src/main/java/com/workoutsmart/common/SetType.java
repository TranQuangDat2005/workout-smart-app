package com.workoutsmart.common;

/** Kiểu hiệp tập — khớp data-model 014 (lưu dưới dạng chuỗi trong DB). */
public enum SetType {
    NORMAL("normal"),
    WARM_UP("warm_up"),
    DROP_SET("drop_set");

    private final String value;

    SetType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (SetType type : values()) {
            if (type.value.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** Trả về giá trị hợp lệ; null hoặc không hợp lệ mặc định là NORMAL. */
    public static String normalize(String value) {
        return isValid(value) ? value : NORMAL.value;
    }
}
