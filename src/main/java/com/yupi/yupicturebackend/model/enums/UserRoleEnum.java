package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Data;
import lombok.Getter;

/**
 * ClassName: UserRoleEnum
 * Package: com.yupi.yupicturebackend.model
 * Description:
 *
 * @Author gjl
 * @Create 2026/4/19 11:24
 * @Version 1.0
 */
@Getter
public enum UserRoleEnum {


    USER("用户", "user"),
    ADMIN("管理员", "admin");


    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举值
     *
     * @param value
     * @return 枚举值
     */
    // 如果枚举值过多，遍历时可以用map缓存所有的枚举值来快速查找，而不是 循环遍历
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anRole : UserRoleEnum.values()) {
            if (anRole.value.equals(value)) {
                return anRole;
            }
        }

        return null;
    }

}
