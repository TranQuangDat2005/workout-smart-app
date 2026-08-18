import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import Spinner from './Spinner';

describe('Spinner', () => {
  it('renders with an accessible label', () => {
    render(<Spinner />);
    expect(screen.getByLabelText('Đang tải')).toBeInTheDocument();
  });
});
