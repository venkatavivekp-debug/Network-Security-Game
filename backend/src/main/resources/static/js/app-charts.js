/* global Chart */
(function () {
  function isFiniteNumber(value) {
    return typeof value === "number" && Number.isFinite(value);
  }

  function sanitizeSeries(series) {
    if (!Array.isArray(series)) {
      return [];
    }
    return series.map((v) => (isFiniteNumber(v) ? v : null));
  }

  function hasRenderableData(labels, datasets) {
    if (!Array.isArray(labels) || labels.length === 0) {
      return false;
    }
    if (!Array.isArray(datasets) || datasets.length === 0) {
      return false;
    }
    return datasets.some((ds) => {
      const data = sanitizeSeries(ds && ds.data);
      return data.some((v) => v !== null);
    });
  }

  function missingChartNotice(canvas, message) {
    if (!canvas || !canvas.parentElement) {
      return;
    }
    const note = document.createElement("p");
    note.className = "chart-empty";
    note.textContent = message || "Chart unavailable.";
    canvas.parentElement.insertBefore(note, canvas);
    canvas.remove();
  }

  function withDefaultChartOptions(options) {
    const base = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: true },
        tooltip: { enabled: true },
      },
    };

    const merged = Object.assign({}, base, options || {});
    merged.plugins = Object.assign({}, base.plugins, (options && options.plugins) || {});
    return merged;
  }

  function createChart(canvasId, type, labels, datasets, options) {
    if (typeof Chart === "undefined") {
      const canvas = document.getElementById(canvasId);
      missingChartNotice(canvas, "Chart library failed to load (offline/CDN blocked). Tables above still show the metrics.");
      return null;
    }

    const canvas = document.getElementById(canvasId);
    if (!canvas) {
      return null;
    }

    const safeLabels = Array.isArray(labels) ? labels : [];
    const safeDatasets = Array.isArray(datasets)
      ? datasets.map((ds) => Object.assign({}, ds, { data: sanitizeSeries(ds && ds.data) }))
      : [];

    if (!hasRenderableData(safeLabels, safeDatasets)) {
      missingChartNotice(canvas, "No chartable data for this view yet.");
      return null;
    }

    const ctx = canvas.getContext("2d");
    return new Chart(ctx, {
      type,
      data: {
        labels: safeLabels,
        datasets: safeDatasets,
      },
      options: withDefaultChartOptions(options),
    });
  }

  window.NsCharts = {
    createChart,
  };
})();
