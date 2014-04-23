var loadNpmTasks = require('load-grunt-tasks');
var timeGrunt = require('time-grunt');

module.exports = function (grunt) {
  grunt.config.init({
    connect: {
      server: {
        options: {
          port: 9000
        }
      }
    }
  });
  loadNpmTasks(grunt);
  timeGrunt(grunt);
  grunt.registerTask("serve", ["connect:server:keepalive"]);
  grunt.registerTask("default", ["serve"]);
};
