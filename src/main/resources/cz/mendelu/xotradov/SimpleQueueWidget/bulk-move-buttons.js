// OnClick code to POST an HTTP request inspired by validateButton() at
// https://github.com/jenkinsci/jenkins/blob/838cafb8fb86fd843b85d2e22e957e09a1ce86a9/war/src/main/webapp/scripts/hudson-behavior.js#L2581
document.querySelectorAll('.BulkMoveRegex_button').forEach(button => {
    button.onclick = function() {
        var parameters = {};
        // ex-jelly: '${it.getMoveTypeName()}'
        parameters['moveType']=button.dataset.var;   // (named via "data-var" in original caller)
        // ex-jelly: '${it.getItemIdName()}'
        parameters['itemId']=findPreviousFormItem(this, 'BulkMoveRegex').value;

        // Tried to use jenkins.baseUrl provided by jenkins.js but it is not global or something?
        fetch(document.head.dataset.rooturl + '/simpleMove/move', {
            method: 'post',
            body: objectToUrlFormEncoded(parameters),
            headers: crumb.wrap({'Content-Type': 'application/x-www-form-urlencoded',})
        })
    }
})

