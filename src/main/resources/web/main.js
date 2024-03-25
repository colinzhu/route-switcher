const recordListData = {
    records: [],
    async loadRecords() {
        const response = await fetch('/rule-manage/api/rules');
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        this.records = await response.json();
//        console.log("records loaded", this.records);
    },
    async deleteRecord(uriPrefix, target) {
        if (!confirm("[" + uriPrefix + "] confirm delete this rule?")) return;
        const rule = { uriPrefix, target };
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
    target: '',
    user: '',
    updateTime: null,
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
    loadItemToUpdate({uriPrefix, target}) {
        this.resetForm();
        this.editMode = 'update'
        this.uriPrefix = uriPrefix;
        this.target = target;
        this.toggleEditForm();
    },
    async submit() {
        try {
            this.isSubmitting = true;
            const recordData = {
                uriPrefix: this.uriPrefix,
                target: this.target,
                user: this.user,
                updateTime: new Date().getTime(), // current milliseconds timestamp
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
        this.target = '';
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
