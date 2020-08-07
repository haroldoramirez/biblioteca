var truncate = document.querySelectorAll("td");
    truncate = [].slice.apply(truncate);
    truncate.forEach(function (elemento, indice) {
    elemento.title = elemento.innerHTML;
});