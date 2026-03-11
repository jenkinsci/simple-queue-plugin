Behaviour.specify(".simple-move-link", "simple-queue-widget", 0, function (element) {
    const dataHolder = document.getElementById("simple-queue-data-holder");
    const url = dataHolder.dataset.moveUrl;
    const viewOwner = dataHolder.dataset.viewOwner;
    const viewName = dataHolder.dataset.viewName;
    element.addEventListener("click", function (event) {
        event.preventDefault();
        const params = new URLSearchParams({
            itemId: element.dataset.itemId,
            viewOwner: viewOwner,
            viewName: viewName,
            moveType: element.dataset.moveType
        });
        fetch(url + "?" + params, {
            method: "POST",
            cache: "no-cache",
            headers: crumb.wrap({
                "Content-Type": "application/x-www-form-urlencoded",
            })
        }).then((response) => {
            if (!response.ok) {
                console.warn(`SimpleQueueWidget move request failed with status ${response.status}`);
            }
        }).catch((error) => console.warn(`SimpleQueueWidget move request failed: ${error}`));
    });
});