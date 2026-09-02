function openTab(tabButton) {
    // Declare all variables
    let tabcontent, tablinks;
    const container = tabButton.closest(".tgp-container")

    // Get all elements with class="tabcontent" and hide them
    tabcontent = container.querySelectorAll(".tgp-tab__content");
    tabcontent.forEach(tab => {
        tab.classList.add("jenkins-hidden");
    })

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = container.querySelectorAll(".tab");
    tablinks.forEach(tab => {
        //reset active class
        tab.classList.remove("active");
    })

    // Show the current tab, and add an "active" class to the button that opened the tab
    document.getElementById(tabButton.dataset.tabUid).classList.remove("jenkins-hidden");
    document.getElementById(tabButton.dataset.selectedTabId).value = tabButton.dataset.tabUid;
    tabButton.parentElement.classList.add("active");
}

function docReady(fn) {
    // see if DOM is already available
    if (document.readyState === "complete" || document.readyState === "interactive") {
        // call on next available tick
        setTimeout(fn, 1);
    } else {
        document.addEventListener("DOMContentLoaded", fn);
    }
}

docReady(function () {
    const tabButtons = document.getElementsByClassName("tgp-tablinks");
    for (let i = 0; i < tabButtons.length; i++) {
        tabButtons[i].addEventListener("click", function (event) {
            openTab(event.currentTarget);
        });
    }
});
