package com.library.util;

/** 权限码与校验。 */
public final class Permission {

    public static final String USER_QUERY = "user:query";
    public static final String LOAN_QUERY = "loan:query";
    public static final String LOAN_STATS = "loan:stats";
    public static final String LOAN_BORROW = "loan:borrow";
    public static final String LOAN_RETURN = "loan:return";
    public static final String LOAN_SELF = "loan:self";
    public static final String BOOK_READ = "book:read";
    public static final String BOOK_ADD = "book:add";
    public static final String BOOK_EDIT = "book:edit";
    public static final String BOOK_DISCARD = "book:discard";
    public static final String COPY_EDIT = "copy:edit";
    public static final String RECOMMEND_ADD = "recommend:add";
    public static final String RECOMMEND_REVIEW = "recommend:review";

    private Permission() {
    }

    public static void require(SessionUser user, String permission) {
        if (user == null) {
            throw new ApiException(401, 2001, "请先登录");
        }
        if (!user.has(permission)) {
            throw new ApiException(403, 2002, "无权执行该操作");
        }
    }
}
