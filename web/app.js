const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
  const response = await fetch(path, {headers: {'Content-Type': 'application/json'}, ...options});
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Request failed');
  return data;
}

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function severityClass(s) { return s.toLowerCase(); }
function statusClass(s) { return s.toLowerCase().replace('_',''); }

function render(data) {
  $('available').textContent = data.availableAmbulances;
  $('pending').textContent = data.pendingCalls;
  $('completed').textContent = data.report.completed;
  $('avgResponse').textContent = `${data.report.avgResponse.toFixed(1)} min`;
  $('serverStatus').textContent = '● Server Online';
  $('serverStatus').className = 'status-pill online';

  const location = $('location');
  if (!location.dataset.ready) {
    location.innerHTML = data.locations.map(x => `<option>${esc(x)}</option>`).join('');
    location.dataset.ready = '1';
  }

  $('ambulances').innerHTML = `<table class="data-table"><thead><tr><th>ID</th><th>Location</th><th>Status</th></tr></thead><tbody>${data.ambulances.map(a => `<tr><td><b>${esc(a.id)}</b></td><td>${esc(a.location)}</td><td><span class="badge ${statusClass(a.status)}">${esc(a.status.replace('_',' '))}</span></td></tr>`).join('')}</tbody></table>`;

  $('hospitals').innerHTML = `<table class="data-table"><thead><tr><th>Hospital</th><th>Location</th><th>Beds</th></tr></thead><tbody>${data.hospitals.map(h => `<tr><td><b>${esc(h.name)}</b></td><td>${esc(h.location)}</td><td>${h.beds > 0 ? `<span class="badge available">${h.beds} free</span>` : `<span class="badge busy">Full</span>`}</td></tr>`).join('')}</tbody></table>`;

  const active = data.ambulances.filter(a => a.status === 'EN_ROUTE');
  $('completeAmbulance').innerHTML = active.length ? active.map(a => `<option value="${esc(a.id)}">${esc(a.id)} — ${esc(a.location)}</option>`).join('') : '<option value="">No active ambulance</option>';
  $('completeBtn').disabled = !active.length;

  if (!data.dispatches.length) {
    $('dispatches').innerHTML = '<div class="empty">No dispatches yet. Report an emergency or run the demo scenario.</div>';
  } else {
    $('dispatches').innerHTML = data.dispatches.map(d => `<div class="dispatch-item"><div class="dispatch-head"><div><strong>Call #${d.callId} · ${esc(d.description)}</strong><div class="dispatch-meta"><span class="${severityClass(d.severity)}">${esc(d.severity)}</span> · ${esc(d.patientLocation)} · ${esc(d.ambulance)} → ${esc(d.hospital)}</div></div><strong>${d.total.toFixed(1)} min</strong></div><div class="route"><b>🚑 Patient route · ${d.response.toFixed(1)} min</b>${esc(d.toPatient)}</div><div class="route"><b>🏥 Hospital route · ${d.transport.toFixed(1)} min</b>${esc(d.toHospital)}</div></div>`).join('');
  }
}

async function refresh() {
  try { render(await api('/api/state')); }
  catch (e) { $('serverStatus').textContent = '● Server Offline'; $('serverStatus').className = 'status-pill offline'; }
}

$('emergencyForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('formMessage').textContent = 'Submitting...';
  try {
    render(await api('/api/emergency', {method:'POST', body: JSON.stringify({location:$('location').value, severity:$('severity').value, description:$('description').value})}));
    $('description').value = '';
    $('formMessage').textContent = 'Emergency added to the priority queue.';
  } catch (e) { $('formMessage').textContent = e.message; }
});

$('dispatchBtn').addEventListener('click', async () => {
  try { render(await api('/api/dispatch', {method:'POST'})); }
  catch (e) { alert(e.message); }
});

$('completeBtn').addEventListener('click', async () => {
  const id = $('completeAmbulance').value;
  if (!id) return;
  try { render(await api('/api/complete', {method:'POST', body:JSON.stringify({ambulanceId:id})})); }
  catch (e) { alert(e.message); }
});

$('demoBtn').addEventListener('click', async () => {
  if (!confirm('Add the four sample emergencies from Main.java and dispatch them?')) return;
  try { render(await api('/api/demo', {method:'POST'})); }
  catch (e) { alert(e.message); }
});

refresh();
setInterval(refresh, 3000);
