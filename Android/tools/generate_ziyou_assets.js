const fs = require('fs');
const path = require('path');

const DEFAULT_SOURCE_DIR = path.resolve(
  __dirname,
  '../../../AICC_Zhou/accessibility-keyboard/resources'
);
const DEFAULT_OUTPUT_DIR = path.resolve(__dirname, '../app/src/main/assets');

const MAX_QUANPIN_KEYS = 12000;
const MAX_ASSOCIATION_KEYS = 8000;
const MAX_CANDIDATES_PER_KEY = 8;
const MAX_ASSOCIATIONS_PER_KEY = 6;
const MAX_PREFIX_LENGTH = 4;
const MAX_PINYIN_KEY_LENGTH = 12;

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function ensureDir(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
}

function sortKeys(entries) {
  return entries.sort((left, right) => {
    if (left[0].length !== right[0].length) {
      return left[0].length - right[0].length;
    }
    if (right[1].length !== left[1].length) {
      return right[1].length - left[1].length;
    }
    return left[0].localeCompare(right[0]);
  });
}

function trimValueList(values, limit) {
  const deduped = [];
  const seen = new Set();
  for (const value of values) {
    if (typeof value !== 'string') {
      continue;
    }
    const trimmed = value.trim();
    if (!trimmed || seen.has(trimmed)) {
      continue;
    }
    seen.add(trimmed);
    deduped.push(trimmed);
    if (deduped.length >= limit) {
      break;
    }
  }
  return deduped;
}

function buildQuanpinSubset(quanpinMap) {
  const filteredEntries = Object.entries(quanpinMap || {}).filter(([key, values]) => {
    return /^[a-z]+$/.test(key)
      && key.length >= 1
      && key.length <= MAX_PINYIN_KEY_LENGTH
      && Array.isArray(values)
      && values.length > 0;
  });

  const sortedEntries = sortKeys(filteredEntries).slice(0, MAX_QUANPIN_KEYS);
  const subset = {};
  for (const [key, values] of sortedEntries) {
    const trimmedValues = trimValueList(values, MAX_CANDIDATES_PER_KEY);
    if (trimmedValues.length > 0) {
      subset[key] = trimmedValues;
    }
  }
  return subset;
}

function buildAssociationSubset(associationMap) {
  const filteredEntries = Object.entries(associationMap || {}).filter(([key, values]) => {
    return typeof key === 'string'
      && key.length >= 1
      && key.length <= MAX_PREFIX_LENGTH
      && Array.isArray(values)
      && values.length > 0;
  });

  const sortedEntries = sortKeys(filteredEntries).slice(0, MAX_ASSOCIATION_KEYS);
  const subset = {};
  for (const [key, values] of sortedEntries) {
    const trimmedValues = trimValueList(values, MAX_ASSOCIATIONS_PER_KEY);
    if (trimmedValues.length > 0) {
      subset[key] = trimmedValues;
    }
  }
  return subset;
}

function main() {
  const sourceDir = process.argv[2]
    ? path.resolve(process.cwd(), process.argv[2])
    : DEFAULT_SOURCE_DIR;
  const outputDir = process.argv[3]
    ? path.resolve(process.cwd(), process.argv[3])
    : DEFAULT_OUTPUT_DIR;

  const dictPath = path.join(sourceDir, 'dict.json');
  const associationPath = path.join(sourceDir, 'association.json');

  if (!fs.existsSync(dictPath) || !fs.existsSync(associationPath)) {
    throw new Error(
      `Reference assets not found. Expected dict.json and association.json under ${sourceDir}`
    );
  }

  const dictionary = readJson(dictPath);
  const associations = readJson(associationPath);

  const quanpin = buildQuanpinSubset(dictionary.quanpin);
  const associationSubset = buildAssociationSubset(associations);

  ensureDir(outputDir);

  const dictOutput = {
    metadata: {
      source: sourceDir,
      generatedAt: new Date().toISOString(),
      quanpinKeys: Object.keys(quanpin).length,
      maxCandidatesPerKey: MAX_CANDIDATES_PER_KEY,
    },
    quanpin,
  };

  const associationOutput = {
    metadata: {
      source: sourceDir,
      generatedAt: new Date().toISOString(),
      associationKeys: Object.keys(associationSubset).length,
      maxAssociationsPerKey: MAX_ASSOCIATIONS_PER_KEY,
    },
    associations: associationSubset,
  };

  fs.writeFileSync(
    path.join(outputDir, 'ziyou_reference_dict.json'),
    JSON.stringify(dictOutput, null, 2)
  );
  fs.writeFileSync(
    path.join(outputDir, 'ziyou_associations.json'),
    JSON.stringify(associationOutput, null, 2)
  );

  console.log(`Generated ${Object.keys(quanpin).length} quanpin keys.`);
  console.log(`Generated ${Object.keys(associationSubset).length} association keys.`);
  console.log(`Output directory: ${outputDir}`);
}

main();
