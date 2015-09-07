if (window.console) {
  window.console.log("Welcome to fotm.info!");
} else {
  window.console = {
    log: function() {}
  };
}

var fotm = {
  // url param manipulation
  // http://stackoverflow.com/a/6021027/283975
  updateQueryString: function (uri, key, value) {
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    var separator = uri.indexOf('?') !== -1 ? "&" : "?";
    if (uri.match(re)) {
      if (value) {
        return uri.replace(re, '$1' + key + "=" + value + '$2');
      } else {
        if (uri.indexOf('&') !== -1) { // there are still other params
          return uri.replace(re, '$1');
        } else { // no other params, return empty
          return uri.replace(re, "");
        }
      }
    } else {
      if (value) {
        return uri + separator + key + "=" + value;
      } else {
        return uri;
      }
    }
  }
};

// Support for span.time-container elements: https://jsfiddle.net/h36zec41/4/
$(function() {
  var fotm = (window.fotm = window.fotm || {});

  var timeContainers = $("span.time-container");

  var updateTime = function() {
    timeContainers.each(function() {
      var $el = $(this);
      var timestamp = $el.attr('data-timestamp');
      var timeagoString = moment(parseInt(timestamp)).fromNow();
      $el.text(timeagoString);
    });
  };

  if (timeContainers.length > 0) {
    console.log("Starting timer updater...");
    updateTime();
    setInterval(updateTime, 60 /* seconds */ * 1000);
  } else {
    console.log("No time tags found.");
  }

  fotm.getFilter = function() {
    var result = [];

    var $filters = $('.setupFilter[data-filterindex]');

    $filters.each(function(i, obj) {
      var classId = $(obj).find(".classFilter *[data-value]").attr("data-value");
      var specId = $(obj).find(".specFilter *[data-value]").attr("data-value");
      result.push({
        classId: classId,
        specId: specId
      });
    });

    return result;
  };

  fotm.setFilter = function(index, classId, specId) {

    var $filter = $('.setupFilter[data-filterindex="'+index+'"]');
    var $classBox = $filter.find(".classFilter");
    var $specBox = $filter.find(".specFilter");

    function update($box, newId) {
      var $field = $box.find('.dropdown-toggle');
      var currentId = parseInt( $field.attr('data-value') ) || 0;

      if (newId != -1 && currentId != newId) {
        $field.find("img").remove();
        var $img = $box.find(".dropdown-item[data-value="+newId+"] img");
        $img.clone().prependTo($field);
        $field.attr("data-value", newId);
        return true;
      } else {
        return false;
      }
    }

    var updated = update($classBox, classId);

    if (updated) {
      update($specBox, 0);

      if (classId == 0) {
        $specBox.find("li").css("display", "");
      } else {
        $specBox.find("li").css("display", "none");
        $specBox.find("li[data-value=0]").css("display", "");
        var specIds = DomainModels.classes[classId].specs;
        for (var i=0; i<specIds.length; ++i) {
          var s = specIds[i];
          $specBox.find("li[data-value=" + s + "]").css("display", "");
        }
      }
    }

    var updatedSpec = update($specBox, specId);

    if (updated || updatedSpec) {
      var filterString = fotm.getFilter().map(function (e) {
        return e.classId + "-" + e.specId
      }).join(",");

      var withFilters = fotm.updateQueryString(window.location.search, "filters", filterString);
      var filterRef = fotm.updateQueryString(withFilters, "page", "");
      $("#filterBtn").attr("href", filterRef);
    }
  };

  // this function controls replacing dropdown selector with appropritate child value/image
  $('.classFilter .dropdown-item').click(function (e) {
    e.preventDefault();

    var $el = $(this);
    var idx = $el.parents(".setupFilter").attr('data-filterindex');
    var value = $el.attr('data-value');

    fotm.setFilter(idx, value, -1);
  });

  $('.specFilter .dropdown-item').click(function (e) {
    e.preventDefault();

    var $el = $(this);
    var idx = $el.parents(".setupFilter").attr('data-filterindex');
    var value = $el.attr('data-value');

    fotm.setFilter(idx, -1, value);
  });
});
