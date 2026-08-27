function openTab(tabButton) {
    // Declare all variables
    let tabcontent, tablinks;

    // Get all elements with class="tabcontent" and hide them
    tabcontent = document.querySelectorAll(".tabcontent");
    tabcontent.forEach(tab => {
        //hide tab content
        tab.style.display = "none";
    })

    // Get all elements with class="tablinks" and remove the class "active"
    tablinks = document.querySelectorAll(".tablinks");
    tablinks.forEach(tab => {
        //reset active class
        tab.classList.remove("active");
    })

    // Show the current tab, and add an "active" class to the button that opened the tab
    document.getElementById(tabButton.dataset.tabUid).style.display = "block";
    document.getElementById("selected-tab-input").value = tabButton.dataset.tabUid;
    tabButton.classList.add("active");
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
    const tabButtons = document.getElementsByClassName("tablinks");
    for (let i = 0; i < tabButtons.length; i++) {
        tabButtons[i].addEventListener("click", function (event) {
            openTab(event.currentTarget);
        });
    }

    const input = document.getElementsByClassName("active");
    if (input[0] != null) {
        openTab(input[0]);
    }
});
