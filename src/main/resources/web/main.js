const recordListData = {
    records: [],
    async loadRecords() {
        const response = await fetch('/rule-manage/api/rules');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        const records = await response.json();
        this.records = records.sort((a, b) => {
            return a.uriPrefix.localeCompare(b.uriPrefix) || a.fromIP.localeCompare(b.fromIP)
        });

//        console.log("records loaded", this.records);
    },
    async deleteRecord({uriPrefix, fromIP}) {
        if (!confirm("[" + uriPrefix + "] confirm delete this rule?")) return;
        const rule = { uriPrefix, fromIP };
        const response = await fetch('/rule-manage/api/rules', {
            method: 'DELETE',
            body: JSON.stringify(rule)
        });
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        console.log("Record deleted:", rule);
        await this.loadRecords();
    }
}

const recordFormData = {
    editMode: '',
    uriPrefix: '',
    fromIP: '',
    targetOptions: '',
    target: '',
    updateBy: '',
    updateTime: null,
    remark: '',
    isSubmitting: false,
    isEditing: false,
    toggleEditForm() {
        this.isEditing = !this.isEditing;
    },
    showCreateForm() {
        this.resetForm();
        this.editMode = 'create';
        this.toggleEditForm();
    },
    loadItemToUpdate({uriPrefix, fromIP, targetOptions, target, remark}) {
        this.resetForm();
        this.editMode = 'update'
        this.uriPrefix = uriPrefix;
        this.fromIP = fromIP;
        this.targetOptions = targetOptions;
        this.target = target;
        this.remark = remark
        this.toggleEditForm();
    },
    async submit() {
        try {
            this.isSubmitting = true;
            const recordData = {
                uriPrefix: this.uriPrefix,
                fromIP: this.fromIP,
                targetOptions: this.targetOptions,
                target: this.target,
                updateBy: this.updateBy,
                updateTime: new Date().getTime(), // current milliseconds timestamp
                remark: this.remark,
            };
            console.log("recordData:", recordData);
            try {
                const response = await fetch('/rule-manage/api/rules', {
                    method: 'POST',
                    body: JSON.stringify(recordData)
                });
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                console.log("Record created / updated:", recordData);
            } catch (error) {
                console.error(error);
                return;
            }
            this.toggleEditForm();
            this.$dispatch('recordupdated');
            this.resetForm();
        } catch (error) {
            console.log("Error submitting form:", error);
        } finally {
            this.isSubmitting = false;
        }
    },
    resetForm() {
        document.getElementById('recordEditForm').reset();
        this.uriPrefix = '';
        this.fromIP = '';
        this.targetOptions = '';
        this.target = '';
        this.updateBy = '';
        this.remark = '';
    },
    cancel() {
        this.resetForm();
        this.toggleEditForm();
    }
};

function initWebConsole() {
    const protocol = location.protocol == "https:" ? "wss:" : "ws:";
    const socket = new WebSocket(protocol + "//" + location.host);
    socket.onmessage = event => {
        const message = document.createElement("div");
        message.textContent = event.data;
        document.getElementById("messages").appendChild(message);
    }
}


document.addEventListener('alpine:init', () => {
    Alpine.data('recordListData', () => recordListData)
    Alpine.data('recordFormData', () => recordFormData)
    initWebConsole();
})
