// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const eslintPluginPrettierRecommended = require('eslint-plugin-prettier/recommended');
const importPlugin = require('eslint-plugin-import');

const noApiImports = {
  // also catches the relative path from inside shared/ (`../../api/…`)
  group: ['**/api/**'],
  message: 'Components use feature models – only stores, mappers and dialog containers import shared/api.',
};

module.exports = tseslint.config(
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
      eslintPluginPrettierRecommended,
      // Extends two more configuration from "import" plugin
      importPlugin.flatConfigs?.recommended,
      importPlugin.flatConfigs?.typescript,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'schbar',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'schbar',
          style: 'kebab-case',
        },
      ],
      // modern angular rules
      '@angular-eslint/prefer-signals': 'error',
      '@angular-eslint/prefer-output-readonly': 'error',
      // import rules
      'import/no-unresolved': 'error',
      // a feature imports only from itself and shared/; shared/ imports no feature
      'import/no-restricted-paths': [
        'error',
        {
          zones: [
            ...['dashboard', 'rfid-readers', 'rfid-tags', 'tools', 'users'].map((feature) => ({
              target: `./src/app/${feature}`,
              from: './src/app',
              except: [`./${feature}`, './shared', './app.routes.ts'],
              message: 'Features never import from another feature – use shared/api and a model of their own.',
            })),
            {
              target: './src/app/shared',
              from: './src/app',
              except: ['./shared'],
              message: 'shared/ belongs to no feature.',
            },
          ],
        },
      ],
      'no-restricted-syntax': [
        'error',
        {
          selector:
            "ImportDeclaration[source.value='zod'] > :matches(ImportSpecifier[imported.name='z'], ImportDefaultSpecifier)",
          message: "Use `import * as z from 'zod'` – `import { z }` pulls all of zod into the bundle.",
        },
      ],
      'import/order': [
        'error',
        {
          'newlines-between': 'always',
          // Valid values: ("builtin" | "external" | "internal" | "unknown" | "parent" | "sibling" | "index" | "object" | "type")[]
          groups: [
            'internal', // angular imports - configured in 'import/internal-regex'
            'unknown', // angular material imports
            'external', // all libraries imports - configured in 'import/external-module-folders'
            'builtin', // internal-library imports
            ['parent', 'sibling', 'index'], // relative paths
          ],
          pathGroups: [
            {
              pattern: '@angular/material/**',
              group: 'unknown',
              position: 'after',
            },
            {
              pattern: '@internal-library', // TODO: change it to your internal library
              group: 'builtin',
            },
          ],
          pathGroupsExcludedImportTypes: ['type', 'object'],
        },
      ],
    },
    settings: {
      'import/internal-regex': '^@angular/',
      'import/external-module-folders': ['node_modules'],
      'import/parsers': {
        '@typescript-eslint/parser': ['.ts'],
      },
      'import/resolver': {
        alias: true, // <-- Added this line to handle TS aliases
        typescript: {
          alwaysTryTypes: true,
          project: ['tsconfig.json'],
        },
        node: {
          extensions: ['.ts'],
        },
      },
    },
  },
  {
    // only stores, mappers and dialog containers talk to the API layer
    files: [
      'src/app/**/*.component.ts',
      'src/app/**/*.component.spec.ts',
      'src/app/**/*.pipe.ts',
      'src/app/**/*.pipe.spec.ts',
    ],
    ignores: ['src/app/shared/dialogs/**/*-dialog-container.component*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [noApiImports],
        },
      ],
    },
  },
  {
    // dumb components talk to the outside only through inputs and outputs (skill `component`);
    // the same rule replaces the one above for these files, so it repeats noApiImports
    files: ['src/app/*/components/**/*.component.ts', 'src/app/shared/components/**/*.component.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@angular/router',
              importNames: ['Router'],
              message: 'Dumb components do not navigate – emit an output, the page navigates.',
            },
            {
              name: '@angular/material/dialog',
              importNames: ['MatDialog'],
              message:
                'Dumb components do not open dialogs – the page does (confirmations: ConfirmDelete/ConfirmActionDialogService).',
            },
            {
              name: '@angular/material/snack-bar',
              message: 'Dumb components do not show snackbars – the page does.',
            },
            {
              name: '@angular/common/http',
              message: 'Dumb components do not load data – the store does.',
            },
          ],
          patterns: [
            noApiImports,
            {
              group: ['**/*.store'],
              message: 'Dumb components get their data as inputs (ViewModel) – only pages inject stores.',
            },
          ],
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility],
    rules: {
      '@angular-eslint/template/prefer-self-closing-tags': 'error',
      '@angular-eslint/template/prefer-control-flow': 'error',
    },
  },
);
