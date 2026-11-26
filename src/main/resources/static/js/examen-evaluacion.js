(() => {
    const formulario = document.getElementById('form-evaluacion');
    if (!formulario) return;

    const campos = [...formulario.querySelectorAll('input[name^="puntaje-"]')];
    const resumen = document.getElementById('resumen-puntajes');
    const visitados = new Set();

    function actualizar() {
        let total = 0;
        let completos = true;
        campos.forEach(campo => {
            let mensaje = '';
            if (campo.validity.badInput || campo.validity.valueMissing) {
                mensaje = 'Ingresa un puntaje numérico.';
            } else if (campo.validity.rangeUnderflow || campo.validity.rangeOverflow) {
                mensaje = `El puntaje debe estar entre 0 y ${campo.max}.`;
            } else if (campo.validity.stepMismatch) {
                mensaje = 'Usa como máximo dos decimales.';
            }
            const visible = visitados.has(campo);
            campo.setAttribute('aria-invalid', String(visible && Boolean(mensaje)));
            document.getElementById(`error-${campo.id}`).textContent = visible ? mensaje : '';
            completos = completos && !mensaje;
            total += Number.isFinite(campo.valueAsNumber) ? Math.round(campo.valueAsNumber * 100) : 0;
        });
        if (!completos) {
            resumen.textContent = 'Completa o corrige los puntajes para calcular el resultado.';
            return;
        }
        const puntaje = total / 100;
        const resultado = puntaje >= Number(resumen.dataset.minimo) ? 'Aprobado' : 'Reprobado';
        resumen.textContent = `Total ${puntaje.toFixed(2)} de ${resumen.dataset.maximo}. Resultado previsto: ${resultado}. Se registra al guardar.`;
    }

    campos.forEach(campo => {
        if (campo.value !== '') visitados.add(campo);
        campo.addEventListener('input', () => { visitados.add(campo); actualizar(); });
        campo.addEventListener('blur', () => { visitados.add(campo); actualizar(); });
        campo.addEventListener('invalid', () => { visitados.add(campo); actualizar(); });
    });
    actualizar();
})();
