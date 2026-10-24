(function () {
    const savedTheme = localStorage.getItem('gajog-theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
})();

document.addEventListener("DOMContentLoaded", function () {
    // 1. Conmutador de Tema Oscuro / Claro
    const btnTheme = document.getElementById('btnThemeToggle');
    const actualizarIconoTema = () => {
        if (!btnTheme) return;
        const actual = document.documentElement.getAttribute('data-theme') || 'dark';
        btnTheme.textContent = actual === 'dark' ? '☀️ Modo claro' : '🌙 Modo oscuro';
    };
    actualizarIconoTema();

    if (btnTheme) {
        btnTheme.addEventListener('click', function () {
            const actual = document.documentElement.getAttribute('data-theme') || 'dark';
            const nuevo = actual === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', nuevo);
            localStorage.setItem('gajog-theme', nuevo);
            actualizarIconoTema();
        });
    }

    // 2. Sidebar colapsable en móvil/tablet
    const sidebar = document.getElementById('gajogSidebar');
    const backdrop = document.getElementById('gajogBackdrop');
    const btnToggleSidebar = document.getElementById('btnSidebarToggle');

    if (btnToggleSidebar && sidebar && backdrop) {
        const toggleMenu = () => {
            sidebar.classList.toggle('show');
            backdrop.classList.toggle('show');
        };
        btnToggleSidebar.addEventListener('click', toggleMenu);
        backdrop.addEventListener('click', toggleMenu);
    }

    // 3. Mostrar automáticamente todos los Toasts presentes
    document.querySelectorAll('.toast').forEach(function (toastEl) {
        const toast = new bootstrap.Toast(toastEl, { delay: 5000 });
        toast.show();
    });

    // 4. Modal de Confirmación reutilizable para formularios con data-confirm
    const modalConfirmEl = document.getElementById('modalConfirmacionGlobal');
    let formPendiente = null;

    if (modalConfirmEl) {
        const bsModal = new bootstrap.Modal(modalConfirmEl);
        const txtMensaje = document.getElementById('modalConfirmacionMensaje');
        const btnAceptar = document.getElementById('btnConfirmacionAceptar');

        document.querySelectorAll('form[data-confirm]').forEach(function (form) {
            form.addEventListener('submit', function (e) {
                if (form.dataset.confirmed === 'true') return;
                e.preventDefault();
                formPendiente = form;
                if (txtMensaje) {
                    txtMensaje.textContent = form.getAttribute('data-confirm');
                }
                bsModal.show();
            });
        });

        if (btnAceptar) {
            btnAceptar.addEventListener('click', function () {
                if (formPendiente) {
                    formPendiente.dataset.confirmed = 'true';
                    formPendiente.submit();
                }
            });
        }
    }

    // 5. Buscador rápido de tablas con atributo data-table-search="#idTabla"
    document.querySelectorAll('[data-table-search]').forEach(function (input) {
        const selector = input.getAttribute('data-table-search');
        const tabla = document.querySelector(selector);
        if (!tabla) return;

        input.addEventListener('input', function () {
            const filtro = this.value.toLowerCase().trim();
            const filas = tabla.querySelectorAll('tbody tr:not(.empty-row)');
            filas.forEach(function (fila) {
                fila.style.display = fila.innerText.toLowerCase().includes(filtro) ? '' : 'none';
            });
        });
    });
});