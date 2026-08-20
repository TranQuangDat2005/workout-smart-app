/* eslint-env node */
/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: 'jsdom',
  setupFiles: ['<rootDir>/src/test/setup.ts'],
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': '<rootDir>/src/test/styleMock.js',
  },
  transform: {
    '^.+\\.(ts|tsx)$': [
      'ts-jest',
      {
        tsconfig: {
          target: 'ES2020',
          jsx: 'react-jsx',
          module: 'commonjs',
          moduleResolution: 'node',
          esModuleInterop: true,
          allowJs: true,
          skipLibCheck: true,
          isolatedModules: true,
        },
      },
    ],
  },
};
