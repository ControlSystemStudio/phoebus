const widdershins = require('widdershins');
const fs = require('fs');

const options = {
  language_tabs: [{ python: "Python" }, { ruby: "Ruby" }]
};

const fileData = fs.readFileSync('api-docs.json', 'utf8');
const swaggerFile = JSON.parse(fileData);

widdershins.convert(swaggerFile, options)
  .then(markdownOutput => {
    // markdownOutput contains the converted markdown
    fs.writeFileSync('out/swagger.md', markdownOutput, 'utf8');
  })
  .catch(err => {
    // handle errors
  });
