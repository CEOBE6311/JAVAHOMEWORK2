(function () {
    "use strict";

    const { createApp } = Vue;
    createApp({
        data() {
            return {
                form: { name: "", password: "", captcha: "" },
                captchaUrl: "api/captcha?t=" + Date.now(),
                error: "",
                loading: false
            };
        },
        mounted() {
            const params = new URLSearchParams(location.search);
            const name = params.get("name");
            if (name) {
                this.form.name = name;
            }
            window.addEventListener("api-error", (event) => {
                this.error = event.detail;
            });
            this.refreshCaptcha();
        },
        methods: {
            refreshCaptcha() {
                this.captchaUrl = "api/captcha?t=" + Date.now();
                // jQuery 只用于简单的 DOM 焦点辅助，API 请求统一用 Axios
                $("#captcha").val("").trigger("focus");
            },
            reset() {
                this.form.name = "";
                this.form.password = "";
                this.form.captcha = "";
                this.error = "";
                this.refreshCaptcha();
                $("#name").trigger("focus");
            },
            async submit() {
                this.error = "";
                if (!this.form.name || !this.form.password || !this.form.captcha) {
                    this.error = "请填写用户名、密码和验证码";
                    return;
                }
                this.loading = true;
                try {
                    await api.post("auth/login", this.form);
                    location.href = "index.html";
                } catch (e) {
                    this.error = e.response && e.response.data && e.response.data.message
                        ? e.response.data.message : (e.message || "登录失败");
                    this.refreshCaptcha();
                    this.form.captcha = "";
                } finally {
                    this.loading = false;
                }
            }
        }
    }).mount("#app");
})();
