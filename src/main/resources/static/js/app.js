// API Base URL
const API_BASE = '/api';

// ==================== TEMPLATES ====================

function openAddTemplate() {
    document.getElementById('templateModalTitle').textContent = 'Add Template';
    document.getElementById('templateForm').reset();
    document.getElementById('templateId').value = '';
}

async function editTemplate(id) {
    try {
        const response = await fetch(`${API_BASE}/templates/${id}`);
        const result = await response.json();
        const template = result.data;

        document.getElementById('templateModalTitle').textContent = 'Edit Template';
        document.getElementById('templateId').value = template.id;
        document.getElementById('templateName').value = template.name;
        document.getElementById('templateSubject').value = template.subject;
        document.getElementById('templateBody').value = template.body;
        document.getElementById('templateDescription').value = template.description || '';

        new bootstrap.Modal(document.getElementById('templateModal')).show();
    } catch (error) {
        alert('Error loading template: ' + error.message);
    }
}

async function saveTemplate() {
    const id = document.getElementById('templateId').value;
    const data = {
        name: document.getElementById('templateName').value,
        subject: document.getElementById('templateSubject').value,
        body: document.getElementById('templateBody').value,
        description: document.getElementById('templateDescription').value
    };

    try {
        const url = id ? `${API_BASE}/templates/${id}` : `${API_BASE}/templates`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            location.reload();
        } else {
            const result = await response.json();
            alert('Error: ' + result.message);
        }
    } catch (error) {
        alert('Error saving template: ' + error.message);
    }
}

async function deleteTemplate(id) {
    if (!confirm('Are you sure you want to delete this template?')) return;

    try {
        const response = await fetch(`${API_BASE}/templates/${id}`, { method: 'DELETE' });
        if (response.ok) {
            location.reload();
        } else {
            alert('Error deleting template');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function previewTemplate(id) {
    try {
        const response = await fetch(`${API_BASE}/templates/${id}`);
        const result = await response.json();
        const template = result.data;

        document.getElementById('previewSubject').textContent = template.subject;
        document.getElementById('previewBody').innerHTML = template.body;

        new bootstrap.Modal(document.getElementById('previewModal')).show();
    } catch (error) {
        alert('Error loading template: ' + error.message);
    }
}

// ==================== RECIPIENTS ====================

function openAddRecipient() {
    document.getElementById('recipientModalTitle').textContent = 'Add Recipient';
    document.getElementById('recipientForm').reset();
    document.getElementById('recipientId').value = '';
}

async function editRecipient(id) {
    try {
        const response = await fetch(`${API_BASE}/recipients/${id}`);
        const result = await response.json();
        const recipient = result.data;

        document.getElementById('recipientModalTitle').textContent = 'Edit Recipient';
        document.getElementById('recipientId').value = recipient.id;
        document.getElementById('recipientEmail').value = recipient.email;
        document.getElementById('recipientName').value = recipient.name || '';
        document.getElementById('recipientCompany').value = recipient.company || '';
        document.getElementById('recipientPosition').value = recipient.position || '';
        document.getElementById('recipientNotes').value = recipient.notes || '';

        new bootstrap.Modal(document.getElementById('recipientModal')).show();
    } catch (error) {
        alert('Error loading recipient: ' + error.message);
    }
}

async function saveRecipient() {
    const id = document.getElementById('recipientId').value;
    const data = {
        email: document.getElementById('recipientEmail').value,
        name: document.getElementById('recipientName').value,
        company: document.getElementById('recipientCompany').value,
        position: document.getElementById('recipientPosition').value,
        notes: document.getElementById('recipientNotes').value
    };

    try {
        const url = id ? `${API_BASE}/recipients/${id}` : `${API_BASE}/recipients`;
        const method = id ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            location.reload();
        } else {
            const result = await response.json();
            alert('Error: ' + result.message);
        }
    } catch (error) {
        alert('Error saving recipient: ' + error.message);
    }
}

async function deleteRecipient(id) {
    if (!confirm('Are you sure you want to delete this recipient?')) return;

    try {
        const response = await fetch(`${API_BASE}/recipients/${id}`, { method: 'DELETE' });
        if (response.ok) {
            location.reload();
        } else {
            alert('Error deleting recipient');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

async function updateStatus(id, status) {
    try {
        const response = await fetch(`${API_BASE}/recipients/${id}/status?status=${status}`, {
            method: 'PATCH'
        });
        if (response.ok) {
            location.reload();
        } else {
            alert('Error updating status');
        }
    } catch (error) {
        alert('Error: ' + error.message);
    }
}

// ==================== SEND EMAILS ====================

function loadTemplatePreview() {
    const select = document.getElementById('selectedTemplate');
    const option = select.options[select.selectedIndex];
    const previewSection = document.getElementById('templatePreviewSection');

    if (option.value) {
        document.getElementById('previewSubjectLine').textContent = option.dataset.subject;
        document.getElementById('previewBodyContent').innerHTML = option.dataset.body;
        previewSection.style.display = 'block';
    } else {
        previewSection.style.display = 'none';
    }
}

function toggleRecipientSelection() {
    const specific = document.getElementById('selectSpecific').checked;
    document.getElementById('recipientCheckboxes').style.display = specific ? 'block' : 'none';
}

async function sendEmails() {
    const templateId = document.getElementById('selectedTemplate').value;
    if (!templateId) {
        alert('Please select a template');
        return;
    }

    const sendToPending = document.getElementById('allPending').checked;
    let recipientIds = [];

    if (!sendToPending) {
        const checkboxes = document.querySelectorAll('.recipient-checkbox:checked');
        recipientIds = Array.from(checkboxes).map(cb => parseInt(cb.value));

        if (recipientIds.length === 0) {
            alert('Please select at least one recipient');
            return;
        }
    }

    if (!confirm(`Are you sure you want to send emails?`)) return;

    try {
        const response = await fetch(`${API_BASE}/emails/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                templateId: parseInt(templateId),
                recipientIds: sendToPending ? [] : recipientIds
            })
        });

        const result = await response.json();

        if (result.success) {
            const logs = result.data;
            const successCount = logs.filter(l => l.status === 'SUCCESS').length;
            const failedCount = logs.filter(l => l.status === 'FAILED').length;

            document.getElementById('resultModalBody').innerHTML = `
                <div class="text-center">
                    <i class="bi bi-check-circle text-success" style="font-size: 3rem;"></i>
                    <h4 class="mt-3">Emails Processed!</h4>
                    <p class="mb-0">
                        <span class="badge bg-success me-2">${successCount} Sent</span>
                        <span class="badge bg-danger">${failedCount} Failed</span>
                    </p>
                </div>
            `;
        } else {
            document.getElementById('resultModalBody').innerHTML = `
                <div class="text-center text-danger">
                    <i class="bi bi-x-circle" style="font-size: 3rem;"></i>
                    <h4 class="mt-3">Error</h4>
                    <p>${result.message}</p>
                </div>
            `;
        }

        new bootstrap.Modal(document.getElementById('resultModal')).show();
    } catch (error) {
        alert('Error sending emails: ' + error.message);
    }
}

// ==================== LOGS ====================

async function viewLogDetails(id) {
    // For now, find the log from the table data
    // In a real app, you'd fetch from API
    alert('Log details feature - fetch log ' + id + ' from API');
}