(function () {
    "use strict";

    const { createApp } = Vue;

    function emptyForm() {
        return {
            name: "", password: "", phone: "", major: "", role: "学生",
            permissionLevel: 1, entryTime: "",
            area: "", duty: "",
            grade: "", college: "", title: ""
        };
    }

    createApp({
        data() {
            return {
                form: emptyForm(),
                error: "",
                loading: false
            };
        },
        mounted() {
            window.addEventListener("api-error", (event) => {
                this.error = event.detail;
            });
        },
        methods: {
            validate() {
                if (!this.form.name || this.form.name.length < 2) return "姓名至少 2 个字符";
                if (!this.form.password || this.form.password.length < 6) return "密码至少 6 个字符";
                if (!/^1[3-9]\d{9}$/.test(this.form.phone)) return "手机号格式不正确";
                if (!this.form.major) return "请填写专业";
                if (this.form.role === "管理员") {
                    if (!this.form.permissionLevel || this.form.permissionLevel < 1 || this.form.permissionLevel > 3) return "权限等级为 1–3";
                    if (!this.form.entryTime) return "请选择入职时间";
                }
                if (this.form.role === "操作员" && (!this.form.area || !this.form.duty)) return "请填写负责区域和职务";
                if (this.form.role === "学生" && (!this.form.grade || !this.form.college)) return "请填写年级和学院";
                if (this.form.role === "教师" && (!this.form.college || !this.form.title)) return "请填写学院和职称";
                return "";
            },
            async submit() {
                this.error = this.validate();
                if (this.error) return;
                this.loading = true;
                try {
                    await api.post("auth/register", this.form);
                    location.href = "login.html?name=" + encodeURIComponent(this.form.name);
                } catch (e) {
                    this.error = e.response && e.response.data && e.response.data.message
                        ? e.response.data.message : (e.message || "注册失败");
                } finally {
                    this.loading = false;
                }
            }
        }
    }).mount("#app");
})();
