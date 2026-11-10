(function () {
    "use strict";

    // 从当前页面路径推导 context path：/LibraryWeb2026/login.html -> /LibraryWeb2026/
    var ctx = window.location.pathname.replace(/[^/]*$/, "");
    var api = axios.create({
        baseURL: ctx + "api",
        timeout: 15000,
        headers: { "Content-Type": "application/json" }
    });

    api.interceptors.response.use(function (response) {
        var body = response.data;
        if (body && typeof body.code !== "undefined" && body.code !== 0) {
            window.dispatchEvent(new CustomEvent("api-error", { detail: body.message || "操作失败" }));
            return Promise.reject(new Error(body.message || "操作失败"));
        }
        return body ? body.data : null;
    }, function (error) {
        var response = error.response;
        var message = response && response.data && response.data.message
            ? response.data.message : "网络异常，请稍后重试";
        var isLoginPage = window.location.pathname.indexOf("login.html") >= 0;
        var isLoginRequest = error.config && error.config.url && error.config.url.indexOf("auth/login") >= 0;

        if (response && response.status === 401 && !isLoginRequest && !isLoginPage) {
            window.location.href = "login.html";
            return Promise.reject(error);
        }
        window.dispatchEvent(new CustomEvent("api-error", { detail: message }));
        return Promise.reject(error);
    });

    window.api = api;
})();
