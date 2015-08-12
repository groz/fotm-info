if (window.console) {
  console.log("Welcome to fotm.info!");
}

// Support for span.time-container elements: https://jsfiddle.net/h36zec41/4/
$(function() {
  var timeContainers = $("span.time-container");

  var updateTime = function() {
    timeContainers.each(function() {
      var el = $(this);
      var timestamp = el.attr('data-timestamp');
      var timeagoString = moment(parseInt(timestamp)).fromNow();
      el.text(timeagoString);
    });
  };

  if (timeContainers.length > 0) {
    console.log("Starting timer updater...");
    updateTime();
    setInterval(updateTime, 30 /* seconds */ * 1000);
  } else {
    console.log("No time tags found.");
  }
});
